package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementation of {@link InventoryService} handling core stock movements.
 * <p>
 * This service orchestrates the physical-to-digital synchronization of warehouse stock.
 * Features include:
 * <ul>
 *     <li><b>Serialized vs Bulk Tracking:</b> Dynamically switches between unit-level
 *     serial tracking and bulk cost-layering based on product configuration[cite: 1].</li>
 *     <li><b>FEFO Reservation:</b> Soft-locks stock for orders based on First-Expiry
 *     logic to minimize waste[cite: 1].</li>
 *     <li><b>Volumetric Putaway:</b> Validates bin capacity (Weight/Volume) in real-time
 *     during inbound shipments[cite: 1].</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final BinRepository binRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Operational Logic:</b> If the product is serialized, it creates N individual
     * records. If bulk, it utilizes cost-layering to group items with identical
     * Price/Batch/Expiry[cite: 1].
     */
    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int remainingQty = request.getQuantity();
        BigDecimal cost = request.getUnitCost() != null ? request.getUnitCost() : product.getCostPrice();
        String batch = request.getBatchNumber() != null ? request.getBatchNumber() : "NONE";

        log.info("Processing receipt: {} units of SKU {}", remainingQty, product.getSku());

        while (remainingQty > 0) {
            StorageBin suggestedBin = findSuitableBin(product, request, product.getUnitVolume(), product.getWeight());
            StorageBin lockedBin = binRepository.findByIdWithLock(suggestedBin.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bin unavailable"));

            // Check if product requires individual serial tracking[cite: 1]
            if (product.isSerialized()) {
                // For serialized items, we put away 1 unit at a time[cite: 1]
                processPutawaySlice(lockedBin, product, request, cost, batch, 1);
                remainingQty -= 1;
            } else {
                // Bulk logic: Fill bin to capacity[cite: 1]
                int binCapacityUnits = calculateMaxUnits(lockedBin, product);
                int qtyToPutAway = Math.min(remainingQty, binCapacityUnits);

                if (qtyToPutAway <= 0) {
                    lockedBin.setStatus(BinStatus.FULL);
                    binRepository.save(lockedBin);
                    continue;
                }

                processPutawaySlice(lockedBin, product, request, cost, batch, qtyToPutAway);
                remainingQty -= qtyToPutAway;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void adjustStock(InventoryAdjustmentRequest request) {
        if (request.getAdjustmentQuantity() == 0) return;

        InventoryItem item = inventoryRepository.findByIdWithLock(request.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        int newQty = item.getQuantity() + request.getAdjustmentQuantity();
        if (newQty < 0) throw new IllegalOperationException("Adjustment leads to negative physical stock.");

        StorageBin bin = binRepository.findByIdWithLock(item.getStorageBin().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));

        syncBinMetrics(bin, item.getProduct(), request.getAdjustmentQuantity());

        item.setQuantity(newQty);
        inventoryRepository.save(item);

        logTransaction(item, TransactionType.ADJUSTMENT, request.getAdjustmentQuantity().longValue(), request.getReasonCode());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implements FEFO (First-Expiry-First-Out) to prioritize stock that expires soonest[cite: 1].
     */
    @Override
    @Transactional
    public void reserveStock(String productId, Integer quantityToReserve) {
        if (quantityToReserve <= 0) throw new BadRequestException("Reservation quantity must be positive.");

        List<InventoryItem> items = inventoryRepository.findAvailableStockByProductFEFO(productId);
        int remainingToReserve = quantityToReserve;

        for (InventoryItem item : items) {
            if (remainingToReserve <= 0) break;

            int available = item.getAvailableQuantity();
            int take = Math.min(available, remainingToReserve);

            if (take > 0) {
                InventoryItem lockedItem = inventoryRepository.findByIdWithLock(item.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stock layer vanished during reservation"));

                lockedItem.setReservedQuantity(lockedItem.getReservedQuantity() + take);
                inventoryRepository.save(lockedItem);
                remainingToReserve -= take;
            }
        }

        if (remainingToReserve > 0) {
            throw new InsufficientStorageException("Shortage: Only " + (quantityToReserve - remainingToReserve) + " units available.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void releaseReservation(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));

        if (item.getReservedQuantity() < quantity) {
            throw new IllegalOperationException("Cannot release more than currently reserved.");
        }

        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        inventoryRepository.save(item);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs final validation to ensure product is still active and status is
     * still 'AVAILABLE' before physical removal[cite: 1].
     */
    @Override
    @Transactional
    public void commitPick(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));

        // Final Safety Checks[cite: 1]
        if (!item.getProduct().isActive()) throw new IllegalOperationException("Product is deactivated.");
        if (item.getStatus() != InventoryStatus.AVAILABLE) throw new IllegalOperationException("Stock is no longer available (Status: " + item.getStatus() + ")");
        if (item.getQuantity() < quantity) throw new IllegalOperationException("Physical stock shortage during commit.");

        item.setQuantity(item.getQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() - quantity);

        StorageBin bin = binRepository.findByIdWithLock(item.getStorageBin().getId()).get();
        syncBinMetrics(bin, item.getProduct(), -quantity);

        inventoryRepository.save(item);
        logTransaction(item, TransactionType.OUTBOUND, (long) -quantity, "ORDER_FULFILLMENT");
    }

    // --- Private Helper Methods ---

    private void processPutawaySlice(StorageBin bin, Product product, ReceivingRequest req, BigDecimal cost, String batch, int qty) {
        InventoryItem item = inventoryRepository
                .findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDate(
                        product.getId(), bin.getId(), batch, cost, req.getExpiryDate())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setStorageBin(bin);
                    newItem.setQuantity(0);
                    newItem.setBatchNumber(batch);
                    newItem.setExpiryDate(req.getExpiryDate());
                    newItem.setPurchasePrice(cost);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + qty);
        inventoryRepository.save(item);

        syncBinMetrics(bin, product, qty);
        logTransaction(item, TransactionType.INBOUND, (long) qty, "GOODS_RECEIPT");
    }

    private void syncBinMetrics(StorageBin bin, Product product, int qtyChange) {
        bin.setCurrentVolumeOccupied(bin.getCurrentVolumeOccupied() + (product.getUnitVolume() * qtyChange));
        bin.setCurrentWeightLoad(bin.getCurrentWeightLoad() + (product.getWeight() * qtyChange));

        if (bin.getCurrentVolumeOccupied() >= (bin.getMaxVolume() * 0.95)) {
            bin.setStatus(BinStatus.FULL);
        } else if (bin.getCurrentVolumeOccupied() <= 0) {
            bin.setStatus(BinStatus.EMPTY);
        } else {
            bin.setStatus(BinStatus.AVAILABLE);
        }
        binRepository.save(bin);
    }

    private int calculateMaxUnits(StorageBin bin, Product p) {
        double freeVol = bin.getMaxVolume() - bin.getCurrentVolumeOccupied();
        double freeWeight = bin.getMaxWeightCapacity() - bin.getCurrentWeightLoad();
        int volCap = (int) Math.floor(freeVol / p.getUnitVolume());
        int weightCap = (int) Math.floor(freeWeight / p.getWeight());
        return Math.max(0, Math.min(volCap, weightCap));
    }

    private StorageBin findSuitableBin(Product product, ReceivingRequest request, double unitVol, double unitWeight) {
        if (request.getStorageBinId() != null && !request.getStorageBinId().isBlank()) {
            return binRepository.findById(request.getStorageBinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested bin not found"));
        }
        String zoneId = product.getCategory().getPreferredZoneId();
        return binRepository.findSmartPutawayBins(product.getId(), zoneId, unitVol, unitWeight)
                .stream().findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No capacity found in preferred zones."));
    }

    private void logTransaction(InventoryItem item, TransactionType type, Long change, String reason) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setInventoryItem(item);
        tx.setType(type);
        tx.setQuantityChange(change);
        tx.setUnitPrice(item.getPurchasePrice());
        tx.setReasonCode(reason);
        transactionRepository.save(tx);
    }
}