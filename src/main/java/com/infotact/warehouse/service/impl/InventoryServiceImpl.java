package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.*;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.BarcodeAuditService;
import com.infotact.warehouse.service.InventoryService;
import com.infotact.warehouse.service.LayoutService;
import com.infotact.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * {@inheritDoc}
 * Enhanced Implementation of {@link InventoryService} for Industry-Grade Operations.
 * <p>
 * Key Features:
 * <ul>
 *     <li><b>Bulk-to-Picking Flow:</b> Prioritizes BULK for receipt and PICK_FACE for fulfillment.</li>
 *     <li><b>Internal Transfers:</b> Supports replenishment moves between storage types.</li>
 *     <li><b>Pessimistic Locking:</b> Prevents race conditions during high-concurrency picking.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final BinRepository binRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final LayoutService layoutService;
    private final BarcodeAuditService auditService;
    private final UserService userService;

    /**
     * {@inheritDoc}
     * REFINED PICKING LOGIC: Only reserves stock from PICK_FACE bins to ensure
     * ground-level efficiency. Uses FEFO (First-Expiry-First-Out).
     */
    @Override
    @Transactional
    public List<InventoryItem> reserveStock(String productId, Integer quantityToReserve) {
        if (quantityToReserve <= 0) throw new BadRequestException("Reservation quantity must be positive.");

        // Repository filters specifically for BinType.PICK_FACE
        List<InventoryItem> items = inventoryRepository.findAvailableStockForPicking(productId);
        List<InventoryItem> reservedLayers = new ArrayList<>();
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
                reservedLayers.add(lockedItem);
            }
        }

        if (remainingToReserve > 0) {
            throw new InsufficientStorageException("Picking Face Shortage: Only " +
                    (quantityToReserve - remainingToReserve) + " units available in accessible bins.");
        }

        return reservedLayers;
    }

    /**
     * {@inheritDoc}
     * INTERNAL TRANSFER: Moves stock from BULK_STORAGE to PICK_FACE for replenishment.
     * Synchronizes metrics for both source and target bins.
     */
    @Override
    @Transactional
    public void internalStockTransfer(String sourceItemId, String targetBinId, Integer quantity) {
        // 1. Secure source and target with Pessimistic Locking to prevent concurrent overfill
        InventoryItem source = inventoryRepository.findByIdWithLock(sourceItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Source stock not found"));

        StorageBin targetBin = binRepository.findByIdWithLock(targetBinId)
                .orElseThrow(() -> new ResourceNotFoundException("Target bin not found"));

        if (source.getQuantity() < quantity)
            throw new IllegalOperationException("Insufficient source stock.");

        // 2. Deduct from Source (e.g., Bulk Rack)
        source.setQuantity(source.getQuantity() - quantity);
        syncBinMetrics(source.getStorageBin(), source.getProduct(), -quantity);
        inventoryRepository.save(source);

        // CHANGE: Use TRANSFER type instead of ADJUSTMENT
        logTransaction(source, TransactionType.TRANSFER, (long) -quantity, "REPLENISHMENT_OUT");

        // 3. Add to Target (e.g., Picking Face)
        processPutawaySlice(targetBin, source.getProduct(), source.getPurchasePrice(),
                source.getBatchNumber(), source.getExpiryDate(), quantity);

        // ADDED: Log the inbound side of the transfer
        logTransaction(source, TransactionType.TRANSFER, (long) quantity, "REPLENISHMENT_IN");

        log.info("Replenishment: Moved {} units of SKU {} from {} to {}",
                quantity, source.getProduct().getSku(),
                source.getStorageBin().getBinCode(), targetBin.getBinCode());
    }

    /**
     * {@inheritDoc}
     * @param request Data containing Product ID, Quantity, and Batch metadata.
     */
    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {
        User operator = userService.getAuthenticatedUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        try {
            int remainingQty = request.getQuantity();
            BigDecimal cost = request.getUnitCost() != null ? request.getUnitCost() : product.getCostPrice();
            String batch = request.getBatchNumber() != null ? request.getBatchNumber() : "NONE";

            while (remainingQty > 0) {
                // Modified findSuitableBin strictly enforces BinType.BULK_STORAGE
                StorageBin suggestedBin = findSuitableBin(product, request, product.getUnitVolume(), product.getWeight());
                StorageBin lockedBin = binRepository.findByIdWithLock(suggestedBin.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Bin unavailable"));

                int binCapacityUnits = calculateMaxUnits(lockedBin, product);
                int qtyToPutAway = Math.min(remainingQty, binCapacityUnits);

                if (qtyToPutAway <= 0) {
                    lockedBin.setStatus(BinStatus.FULL);
                    binRepository.save(lockedBin);
                    continue;
                }

                processPutawaySlice(lockedBin, product, cost, batch, request.getExpiryDate(), qtyToPutAway);
                remainingQty -= qtyToPutAway;

                auditService.logSuccess(operator.getId(), operator.getWarehouse().getId(),
                        lockedBin.getId(), null, AuditAction.RECEIVING, "SKU: " + product.getSku());
            }
        } catch (Exception e) {
            auditService.logFailure(operator.getId(), operator.getWarehouse().getId(),
                    request.getStorageBinId(), null, AuditAction.RECEIVING, "QTY: " + request.getQuantity(), e.getMessage());
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * @param inventoryItemId The digital record ID.
     * @param scannedBinCode The raw barcode text from the physical rack scan.
     * @param scannedSku The raw barcode text from the product sticker scan.
     * @param quantity The quantity physically picked.
     */
    @Override
    @Transactional
    public void commitPickWithVerification(String inventoryItemId, String scannedBinCode, String scannedSku, Integer quantity) {
        User operator = userService.getAuthenticatedUser();
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        try {
            if (!layoutService.verifyBinScan(scannedBinCode, item.getStorageBin().getId())) {
                throw new IllegalOperationException("Verification Failed: Incorrect bin location barcode.");
            }

            if (!item.getProduct().getSku().equalsIgnoreCase(scannedSku)) {
                throw new IllegalOperationException("Verification Failed: Product SKU mismatch.");
            }

            commitPick(inventoryItemId, quantity);

            auditService.logSuccess(operator.getId(), operator.getWarehouse().getId(),
                    item.getStorageBin().getId(), null, AuditAction.PICKING, scannedBinCode);

        } catch (IllegalOperationException e) {
            auditService.logFailure(operator.getId(), operator.getWarehouse().getId(),
                    item.getStorageBin().getId(), null, AuditAction.PICKING, scannedBinCode, e.getMessage());
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     * @param inventoryItemId The ID of the inventory record being picked.
     * @param quantity The amount being removed from the warehouse.
     */
    @Override
    @Transactional
    public void commitPick(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));

        if (!item.getProduct().isActive()) throw new IllegalOperationException("Product is deactivated.");
        if (item.getStatus() != InventoryStatus.AVAILABLE) throw new IllegalOperationException("Stock status is " + item.getStatus());
        if (item.getQuantity() < quantity) throw new IllegalOperationException("Physical stock shortage.");

        item.setQuantity(item.getQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() - quantity);

        StorageBin bin = binRepository.findByIdWithLock(item.getStorageBin().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));

        syncBinMetrics(bin, item.getProduct(), -quantity);
        inventoryRepository.save(item);
        logTransaction(item, TransactionType.OUTBOUND, (long) -quantity, "VERIFIED_FULFILLMENT");
    }

    /**
     * {@inheritDoc}
     * @param request Payload containing Inventory UUID, adjustment delta, and reason.
     */
    @Override
    @Transactional
    public void adjustStock(InventoryAdjustmentRequest request) {
        User manager = userService.getAuthenticatedUser();
        InventoryItem item = inventoryRepository.findByIdWithLock(request.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        int newQty = item.getQuantity() + request.getAdjustmentQuantity();
        if (newQty < 0) throw new IllegalOperationException("Negative stock balance not permitted.");

        StorageBin bin = binRepository.findByIdWithLock(item.getStorageBin().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));

        syncBinMetrics(bin, item.getProduct(), request.getAdjustmentQuantity());
        item.setQuantity(newQty);
        inventoryRepository.save(item);

        logTransaction(item, TransactionType.ADJUSTMENT, request.getAdjustmentQuantity().longValue(), request.getReasonCode());
        auditService.logSuccess(manager.getId(), manager.getWarehouse().getId(),
                bin.getId(), null, AuditAction.STOCK_ADJUSTMENT, "Delta: " + request.getAdjustmentQuantity());
    }

    /**
     * {@inheritDoc}
     * @param inventoryItemId The digital record ID to release.
     * @param quantity The quantity to return to 'Available' status.
     */
    @Override
    @Transactional
    public void releaseReservation(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));

        if (item.getReservedQuantity() < quantity) throw new IllegalOperationException("Cannot release more than reserved.");

        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        inventoryRepository.save(item);
    }

    // --- Private Industrial Helpers ---

    private void processPutawaySlice(StorageBin bin, Product product, BigDecimal cost, String batch, LocalDate expiry, int qty) {
        InventoryItem item = inventoryRepository
                .findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDate(
                        product.getId(), bin.getId(), batch, cost, expiry)
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setStorageBin(bin);
                    newItem.setQuantity(0);
                    newItem.setBatchNumber(batch);
                    newItem.setExpiryDate(expiry);
                    newItem.setPurchasePrice(cost);
                    newItem.setStatus(InventoryStatus.AVAILABLE);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + qty);
        inventoryRepository.save(item);
        syncBinMetrics(bin, product, qty);
        logTransaction(item, TransactionType.INBOUND, (long) qty, "STORAGE_UPDATE");
    }

    private void syncBinMetrics(StorageBin bin, Product product, int qtyChange) {
        BigDecimal deltaVolume = product.getUnitVolume().multiply(BigDecimal.valueOf(qtyChange));
        bin.setCurrentVolumeOccupied(bin.getCurrentVolumeOccupied() + deltaVolume.doubleValue());
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
        int volCap = (int) Math.floor(freeVol / p.getUnitVolume().doubleValue());
        int weightCap = (int) Math.floor(freeWeight / p.getWeight());
        return Math.max(0, Math.min(volCap, weightCap));
    }

    private StorageBin findSuitableBin(Product product, ReceivingRequest request, BigDecimal unitVol, double unitWeight) {
        if (request.getStorageBinId() != null && !request.getStorageBinId().isBlank()) {
            return binRepository.findById(request.getStorageBinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested bin not found"));
        }

        // Logic: New inbound stock always defaults to BULK STORAGE
        BinType targetType = BinType.BULK_STORAGE;

        return binRepository.findSmartPutawayBins(product.getId(), null, unitVol, unitWeight, targetType)
                .stream().findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No capacity available in BULK STORAGE zones."));
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