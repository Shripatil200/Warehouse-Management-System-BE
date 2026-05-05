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
     * Enhanced Reservation Logic with Auto-Replenishment.
     * If PICK_FACE stock is insufficient, it attempts to pull from BULK_STORAGE
     * before finalizing the reservation.
     */
    @Override
    @Transactional
    public List<InventoryItem> reserveStock(String productId, Integer quantityToReserve) {
        if (quantityToReserve <= 0) throw new BadRequestException("Reservation quantity must be positive.");

        // 1. Initial check of Picking Face stock
        List<InventoryItem> items = inventoryRepository.findAvailableStockForPicking(
                productId, InventoryStatus.AVAILABLE, BinType.PICK_FACE);

        int currentAvailable = items.stream().mapToInt(InventoryItem::getAvailableQuantity).sum();

        // 2. TRIGGER REPLENISHMENT: If stock is low, pull from Bulk
        if (currentAvailable < quantityToReserve) {
            log.info("Low stock in Pick Face for SKU {}. Attempting auto-replenishment...", productId);

            // Find a suitable target bin in the Picking Face to move stock into
            StorageBin targetPickingBin = items.isEmpty() ?
                    findPickingBinForProduct(productId) : items.get(0).getStorageBin();

            int deficit = quantityToReserve - currentAvailable;

            try {
                replenishPickingFace(productId, targetPickingBin.getId(), deficit);

                // Refresh the item list after replenishment is successful
                items = inventoryRepository.findAvailableStockForPicking(
                        productId, InventoryStatus.AVAILABLE, BinType.PICK_FACE);
            } catch (InsufficientStorageException e) {
                log.error("Auto-replenishment failed: No bulk stock available to cover deficit.");
            }
        }

        // 3. Proceed with Reservation
        List<InventoryItem> reservedLayers = new ArrayList<>();
        int remainingToReserve = quantityToReserve;

        for (InventoryItem item : items) {
            if (remainingToReserve <= 0) break;
            int available = item.getAvailableQuantity();
            int take = Math.min(available, remainingToReserve);

            if (take > 0) {
                InventoryItem lockedItem = inventoryRepository.findByIdWithLock(item.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stock record locked by another process"));

                lockedItem.setReservedQuantity(lockedItem.getReservedQuantity() + take);
                inventoryRepository.save(lockedItem);

                remainingToReserve -= take;
                reservedLayers.add(lockedItem);
            }
        }

        // Final check: if still short after replenishment attempt, throw exception
        if (remainingToReserve > 0) {
            throw new InsufficientStorageException("Absolute Stock Shortage: Total warehouse stock (Bulk + Pick) " +
                    "cannot fulfill the requested " + quantityToReserve + " units.");
        }

        return reservedLayers;
    }

    /**
     * Helper to find a fallback Picking Bin if the product has no current stock in PICK_FACE.
     */
    private StorageBin findPickingBinForProduct(String productId) {
        return binRepository.findSmartPutawayBins(
                        productId,
                        null,
                        BigDecimal.ZERO,
                        0.0,
                        BinType.PICK_FACE,
                        BinStatus.AVAILABLE // Added this
                )
                .stream().findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No designated Picking Face bin found for replenishment."));
    }

    @Override
    @Transactional
    public void internalStockTransfer(String sourceItemId, String targetBinId, Integer quantity) {
        InventoryItem source = inventoryRepository.findByIdWithLock(sourceItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Source stock not found"));
        StorageBin targetBin = binRepository.findByIdWithLock(targetBinId)
                .orElseThrow(() -> new ResourceNotFoundException("Target bin not found"));

        if (source.getQuantity() < quantity) throw new IllegalOperationException("Insufficient source stock.");

        source.setQuantity(source.getQuantity() - quantity);
        syncBinMetrics(source.getStorageBin(), source.getProduct(), -quantity);
        inventoryRepository.save(source);

        logTransaction(source, TransactionType.TRANSFER, (long) -quantity, "TRANSFER_OUT");

        processPutawaySlice(targetBin, source.getProduct(), source.getPurchasePrice(),
                source.getBatchNumber(), source.getExpiryDate(), quantity);

        logTransaction(source, TransactionType.TRANSFER, (long) quantity, "TRANSFER_IN");
    }

    @Override
    @Transactional
    public void replenishPickingFace(String productId, String targetBinId, Integer desiredQty) {
        // FIXED: Using Enum parameters for the new Repository signature
        List<InventoryItem> bulkSources = inventoryRepository.findBulkSourceForReplenishment(
                productId, InventoryStatus.AVAILABLE, BinType.BULK_STORAGE);

        if (bulkSources.isEmpty()) {
            throw new InsufficientStorageException("Replenishment failed: No bulk stock available.");
        }

        int remainingToMove = desiredQty;
        for (InventoryItem sourceItem : bulkSources) {
            if (remainingToMove <= 0) break;
            int take = Math.min(sourceItem.getQuantity(), remainingToMove);
            internalStockTransfer(sourceItem.getId(), targetBinId, take);
            remainingToMove -= take;
        }
    }

    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {
        User operator = userService.getAuthenticatedUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int remainingQty = request.getQuantity();
        BigDecimal cost = request.getUnitCost() != null ? request.getUnitCost() : product.getCostPrice();
        String batch = request.getBatchNumber() != null ? request.getBatchNumber() : "NONE";

        while (remainingQty > 0) {
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
    }

    @Override
    @Transactional
    public void commitPickWithVerification(String inventoryItemId, String scannedBinCode, String scannedSku, Integer quantity) {
        InventoryItem item = inventoryRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (!layoutService.verifyBinScan(scannedBinCode, item.getStorageBin().getId())) {
            throw new IllegalOperationException("Incorrect bin location barcode.");
        }
        if (!item.getProduct().getSku().equalsIgnoreCase(scannedSku)) {
            throw new IllegalOperationException("Product SKU mismatch.");
        }

        commitPick(inventoryItemId, quantity);
    }

    @Override
    @Transactional
    public void commitPick(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));

        item.setQuantity(item.getQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        syncBinMetrics(item.getStorageBin(), item.getProduct(), -quantity);
        inventoryRepository.save(item);
        logTransaction(item, TransactionType.OUTBOUND, (long) -quantity, "PICK_COMPLETE");
    }

    @Override
    @Transactional
    public void adjustStock(InventoryAdjustmentRequest request) {
        InventoryItem item = inventoryRepository.findByIdWithLock(request.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        int delta = request.getAdjustmentQuantity();
        item.setQuantity(item.getQuantity() + delta);
        syncBinMetrics(item.getStorageBin(), item.getProduct(), delta);
        inventoryRepository.save(item);
        logTransaction(item, TransactionType.ADJUSTMENT, (long) delta, request.getReasonCode());
    }

    @Override
    @Transactional
    public void releaseReservation(String inventoryItemId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        inventoryRepository.save(item);
    }

    // --- Helpers ---

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
        logTransaction(item, TransactionType.INBOUND, (long) qty, "PUTAWAY");
    }

    private void syncBinMetrics(StorageBin bin, Product product, int qtyChange) {
        BigDecimal deltaVol = product.getUnitVolume().multiply(BigDecimal.valueOf(qtyChange));
        bin.setCurrentVolumeOccupied(bin.getCurrentVolumeOccupied() + deltaVol.doubleValue());
        bin.setCurrentWeightLoad(bin.getCurrentWeightLoad() + (product.getWeight() * qtyChange));
        binRepository.save(bin);
    }

    private int calculateMaxUnits(StorageBin bin, Product p) {
        double freeVol = bin.getMaxVolume() - bin.getCurrentVolumeOccupied();
        int volCap = (int) Math.floor(freeVol / p.getUnitVolume().doubleValue());
        return Math.max(0, volCap);
    }

    private StorageBin findSuitableBin(Product product, ReceivingRequest request, BigDecimal unitVol, double unitWeight) {
        if (request.getStorageBinId() != null && !request.getStorageBinId().isBlank()) {
            return binRepository.findById(request.getStorageBinId()).orElseThrow();
        }

        return binRepository.findSmartPutawayBins(
                        product.getId(),
                        null,
                        unitVol,
                        unitWeight,
                        BinType.BULK_STORAGE,
                        BinStatus.AVAILABLE // Added this
                )
                .stream().findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No Bulk Capacity"));
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