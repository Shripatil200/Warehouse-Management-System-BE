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
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Implementation of {@link InventoryService} for Industry-Grade Operations.
 * Integrates FEFO reservation logic and Barcode Audit tracking.
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
     * FEFO Reservation returns specific layers for picker guidance.
     */
    @Override
    @Transactional
    public List<InventoryItem> reserveStock(String productId, Integer quantityToReserve) {
        if (quantityToReserve <= 0) throw new BadRequestException("Reservation quantity must be positive.");

        List<InventoryItem> items = inventoryRepository.findAvailableStockByProductFEFO(productId);
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
            throw new InsufficientStorageException("Shortage: Only " + (quantityToReserve - remainingToReserve) + " units available.");
        }

        return reservedLayers;
    }

    /**
     * Industry-Ready Pick Commitment with Physical Verification and Audit Logging.
     */
    @Override
    @Transactional
    public void commitPickWithVerification(String inventoryItemId, String scannedBinCode, String scannedSku, Integer quantity) {
        User operator = userService.getAuthenticatedUser();
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        try {
            // 1. Verify physical location barcode
            boolean isCorrectBin = layoutService.verifyBinScan(scannedBinCode, item.getStorageBin().getId());
            if (!isCorrectBin) {
                throw new IllegalOperationException("Verification Failed: You are at the wrong bin location.");
            }

            // 2. Verify physical product barcode via SKU
            if (!item.getProduct().getSku().equalsIgnoreCase(scannedSku)) {
                throw new IllegalOperationException("Verification Failed: Scanned product does not match SKU.");
            }

            // 3. Physical Deduction
            commitPick(inventoryItemId, quantity);

            // AUDIT SUCCESS
            auditService.logSuccess(operator.getId(), operator.getWarehouse().getId(),
                    item.getStorageBin().getId(), null, AuditAction.PICKING, scannedBinCode);

        } catch (IllegalOperationException e) {
            // AUDIT FAILURE
            auditService.logFailure(operator.getId(), operator.getWarehouse().getId(),
                    item.getStorageBin().getId(), null, AuditAction.PICKING, scannedBinCode, e.getMessage());
            throw e;
        }
    }

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
                StorageBin suggestedBin = findSuitableBin(product, request, product.getUnitVolume(), product.getWeight());
                StorageBin lockedBin = binRepository.findByIdWithLock(suggestedBin.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Bin unavailable"));

                int qtyToPutAway;
                if (product.isSerialized()) {
                    qtyToPutAway = 1;
                } else {
                    int binCapacityUnits = calculateMaxUnits(lockedBin, product);
                    qtyToPutAway = Math.min(remainingQty, binCapacityUnits);

                    if (qtyToPutAway <= 0) {
                        lockedBin.setStatus(BinStatus.FULL);
                        binRepository.save(lockedBin);
                        continue;
                    }
                }

                processPutawaySlice(lockedBin, product, request, cost, batch, qtyToPutAway);
                remainingQty -= qtyToPutAway;

                // AUDIT SUCCESS for each slice
                auditService.logSuccess(operator.getId(), operator.getWarehouse().getId(),
                        lockedBin.getId(), null, AuditAction.RECEIVING, "SKU: " + product.getSku());
            }
        } catch (Exception e) {
            auditService.logFailure(operator.getId(), operator.getWarehouse().getId(),
                    request.getStorageBinId(), null, AuditAction.RECEIVING, "QTY: " + request.getQuantity(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void adjustStock(InventoryAdjustmentRequest request) {
        User manager = userService.getAuthenticatedUser();
        if (request.getAdjustmentQuantity() == 0) return;

        InventoryItem item = inventoryRepository.findByIdWithLock(request.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        try {
            int newQty = item.getQuantity() + request.getAdjustmentQuantity();
            if (newQty < 0) throw new IllegalOperationException("Adjustment results in negative stock.");

            StorageBin bin = binRepository.findByIdWithLock(item.getStorageBin().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));

            syncBinMetrics(bin, item.getProduct(), request.getAdjustmentQuantity());

            item.setQuantity(newQty);
            inventoryRepository.save(item);

            logTransaction(item, TransactionType.ADJUSTMENT, request.getAdjustmentQuantity().longValue(), request.getReasonCode());

            auditService.logSuccess(manager.getId(), manager.getWarehouse().getId(),
                    bin.getId(), null, AuditAction.STOCK_ADJUSTMENT, "Delta: " + request.getAdjustmentQuantity());

        } catch (Exception e) {
            auditService.logFailure(manager.getId(), manager.getWarehouse().getId(),
                    item.getStorageBin().getId(), null, AuditAction.STOCK_ADJUSTMENT, "FAILED_ADJ", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void releaseReservation(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));

        if (item.getReservedQuantity() < quantity) {
            throw new IllegalOperationException("Cannot release more than reserved.");
        }

        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        inventoryRepository.save(item);
    }

    // --- Private Utility Helpers ---

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
                    newItem.setStatus(InventoryStatus.AVAILABLE);
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
                    .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));
        }
        String zoneId = product.getCategory().getPreferredZoneId();
        return binRepository.findSmartPutawayBins(product.getId(), zoneId, unitVol, unitWeight)
                .stream().findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No capacity in preferred zones."));
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