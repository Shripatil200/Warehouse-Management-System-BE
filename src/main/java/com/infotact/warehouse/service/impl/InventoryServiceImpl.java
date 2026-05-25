package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.*;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.BarcodeAuditService;
import com.infotact.warehouse.service.InventoryService;
import com.infotact.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final BinRepository binRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final BarcodeAuditService auditService;
    private final UserService userService;

    // ============================================================
    // RECEIVE (SMART PUTAWAY - MULTI BIN)
    // ============================================================

    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {

        String warehouseId = getWarehouseId();
        User operator = userService.getAuthenticatedUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow();

        int remaining = request.getQuantity();

        BigDecimal cost = Optional.ofNullable(request.getUnitCost())
                .orElse(product.getCostPrice());

        String batch = Optional.ofNullable(request.getBatchNumber()).orElse("NONE");

        List<StorageBin> bins = binRepository.findPutawayCandidates(
                product.getId(),
                null,
                product.getUnitVolume(),
                product.getWeight(),
                BinType.BULK_STORAGE,
                BinStatus.AVAILABLE,
                warehouseId,
                ZoneType.RECEIVING
        );

        if (bins.isEmpty())
            throw new InsufficientStorageException("No bins available");

        for (StorageBin bin : bins) {

            if (remaining <= 0) break;

            StorageBin locked = binRepository.findByIdWithLock(bin.getId(), warehouseId)
                    .orElseThrow();

            int capacity = calculateMaxUnits(locked, product);
            if (capacity <= 0) continue;

            int putQty = Math.min(remaining, capacity);

            processPutawaySlice(locked, product, cost, batch,
                    request.getExpiryDate(), putQty, warehouseId);

            remaining -= putQty;

            // Bin status (FULL/AVAILABLE) is now managed centrally in syncBinMetrics

            auditService.logSuccess(
                    operator.getId(),
                    locked.getId(),
                    null,
                    AuditAction.RECEIVING,
                    product.getSku()
            );
        }

        if (remaining > 0)
            throw new InsufficientStorageException("Remaining qty: " + remaining);
    }

    // ============================================================
    // RESERVE STOCK (FEFO)
    // ============================================================

    @Override
    @Transactional
    public List<InventoryItem> reserveStock(String productId, Integer quantity) {

        if (quantity <= 0)
            throw new BadRequestException("Reservation must be positive");

        String warehouseId = getWarehouseId();

        List<InventoryItem> reserved = new ArrayList<>();
        int remaining = quantity;

        List<InventoryItem> layers = inventoryRepository.findAvailableStockForPicking(
                productId,
                InventoryStatus.AVAILABLE,
                BinType.PICK_FACE,
                warehouseId
        );

        remaining = reserveFromLayers(layers, remaining, reserved, warehouseId);

        if (remaining > 0) {
            StorageBin pickingBin = findSuitableBin(productId, warehouseId, ZoneType.PICKING, BinType.PICK_FACE);
            replenishPickingFace(productId, pickingBin.getId(), remaining);

            List<InventoryItem> refreshed = inventoryRepository.findAvailableStockForPicking(
                    productId,
                    InventoryStatus.AVAILABLE,
                    BinType.PICK_FACE,
                    warehouseId
            );

            remaining = reserveFromLayers(refreshed, remaining, reserved, warehouseId);
        }

        if (remaining > 0)
            throw new InsufficientStorageException("Insufficient stock");

        return reserved;
    }

    private int reserveFromLayers(List<InventoryItem> items, int remaining,
                                  List<InventoryItem> reserved, String warehouseId) {

        for (InventoryItem item : items) {
            if (remaining <= 0) break;

            InventoryItem locked = inventoryRepository.findByIdWithLock(item.getId(), warehouseId)
                    .orElseThrow();

            int available = locked.getAvailableQuantity();
            if (available <= 0) continue;

            int take = Math.min(available, remaining);

            locked.setReservedQuantity(locked.getReservedQuantity() + take);
            inventoryRepository.save(locked);

            reserved.add(locked);
            remaining -= take;
        }

        return remaining;
    }

    // ============================================================
    // RELEASE
    // ============================================================

    @Override
    @Transactional
    public void releaseReservation(String inventoryItemId, Integer quantity) {

        String warehouseId = getWarehouseId();

        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId, warehouseId)
                .orElseThrow();

        if (item.getReservedQuantity() < quantity)
            throw new IllegalOperationException("Invalid release");

        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        inventoryRepository.save(item);
    }

    // ============================================================
    // PICK WITH VERIFICATION
    // ============================================================

    @Override
    @Transactional
    public void commitPickWithVerification(String inventoryItemId,
                                           String scannedBinCode,
                                           String scannedSku,
                                           Integer quantity) {

        String warehouseId = getWarehouseId();

        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId, warehouseId)
                .orElseThrow();

        if (!item.getStorageBin().getBinCode().equals(scannedBinCode))
            throw new IllegalOperationException("Wrong bin scanned");

        if (!item.getProduct().getSku().equals(scannedSku))
            throw new IllegalOperationException("Wrong SKU scanned");

        commitPick(inventoryItemId, quantity);
    }

    // ============================================================
    // PICK
    // ============================================================

    @Override
    @Transactional
    public void commitPick(String inventoryItemId, Integer quantity) {

        String warehouseId = getWarehouseId();

        InventoryItem item = inventoryRepository.findByIdWithLock(inventoryItemId, warehouseId)
                .orElseThrow();

        if (item.getReservedQuantity() < quantity)
            throw new IllegalOperationException("Over-pick");

        item.setQuantity(item.getQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() - quantity);

        inventoryRepository.save(item);

        syncBinMetrics(item.getStorageBin(), item.getProduct(), -quantity, warehouseId);

        logTransaction(item, TransactionType.OUTBOUND, (long) -quantity, "PICK");
    }

    // ============================================================
    // ADJUST
    // ============================================================

    @Override
    @Transactional
    public void adjustStock(InventoryAdjustmentRequest request) {

        String warehouseId = getWarehouseId();

        InventoryItem item = inventoryRepository.findByIdWithLock(request.getInventoryItemId(), warehouseId)
                .orElseThrow();

        int newQty = item.getQuantity() + request.getAdjustmentQuantity();

        if (newQty < 0)
            throw new IllegalOperationException("Negative stock");

        item.setQuantity(newQty);

        inventoryRepository.save(item);

        syncBinMetrics(item.getStorageBin(), item.getProduct(),
                request.getAdjustmentQuantity(), warehouseId);

        logTransaction(item, TransactionType.ADJUSTMENT,
                (long) request.getAdjustmentQuantity(),
                request.getReasonCode());
    }

    // ============================================================
    // TRANSFER
    // ============================================================

    @Override
    @Transactional
    public void internalStockTransfer(String sourceItemId, String targetBinId, Integer quantity) {

        String warehouseId = getWarehouseId();

        InventoryItem source = inventoryRepository.findByIdWithLock(sourceItemId, warehouseId)
                .orElseThrow();

        if (source.getAvailableQuantity() < quantity)
            throw new IllegalOperationException("Insufficient stock");

        StorageBin target = binRepository.findByIdWithLock(targetBinId, warehouseId)
                .orElseThrow();

        source.setQuantity(source.getQuantity() - quantity);
        inventoryRepository.save(source);

        syncBinMetrics(source.getStorageBin(), source.getProduct(), -quantity, warehouseId);

        processPutawaySlice(target, source.getProduct(),
                source.getPurchasePrice(),
                source.getBatchNumber(),
                source.getExpiryDate(),
                quantity,
                warehouseId);
    }

    // ============================================================
    // REPLENISHMENT
    // ============================================================

    @Override
    @Transactional
    public void replenishPickingFace(String productId, String targetBinId, Integer qty) {

        String warehouseId = getWarehouseId();

        List<InventoryItem> bulk = inventoryRepository.findBulkSourceForReplenishment(
                productId,
                InventoryStatus.AVAILABLE,
                BinType.BULK_STORAGE,
                warehouseId
        );

        int remaining = qty;

        for (InventoryItem item : bulk) {
            if (remaining <= 0) break;

            int available = item.getAvailableQuantity();
            if (available <= 0) continue;

            int take = Math.min(available, remaining);

            internalStockTransfer(item.getId(), targetBinId, take);
            remaining -= take;
        }

        if (remaining > 0)
            throw new InsufficientStorageException("Not enough bulk stock");
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private StorageBin findSuitableBin(String productId, String warehouseId,
                                       ZoneType zoneType, BinType binType) {

        return binRepository.findPutawayCandidates(
                        productId,
                        null,
                        BigDecimal.ZERO,
                        0.0,
                        binType,
                        BinStatus.AVAILABLE,
                        warehouseId,
                        zoneType
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No bin found"));
    }

    private int calculateMaxUnits(StorageBin bin, Product product) {
        double remainingVolume = bin.getMaxVolume() - bin.getCurrentVolumeOccupied();
        double remainingWeight = bin.getMaxWeightCapacity() - bin.getCurrentWeightLoad();

        int byVolume = (int) (remainingVolume / product.getUnitVolume().doubleValue());
        int byWeight = (int) (remainingWeight / product.getWeight());

        return Math.max(0, Math.min(byVolume, byWeight));
    }

    private void processPutawaySlice(StorageBin bin,
                                     Product product,
                                     BigDecimal cost,
                                     String batch,
                                     LocalDate expiry,
                                     int qty,
                                     String warehouseId) {

        InventoryItem item = inventoryRepository
                .findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDateAndStorageBin_Warehouse_Id(
                        product.getId(), bin.getId(), batch, cost, expiry, warehouseId
                )
                .orElseGet(() -> {
                    InventoryItem i = new InventoryItem();
                    i.setProduct(product);
                    i.setStorageBin(bin);
                    i.setBatchNumber(batch);
                    i.setExpiryDate(expiry);
                    i.setPurchasePrice(cost);
                    i.setQuantity(0);
                    i.setStatus(InventoryStatus.AVAILABLE);
                    return i;
                });

        item.setQuantity(item.getQuantity() + qty);
        inventoryRepository.save(item);

        syncBinMetrics(bin, product, qty, warehouseId);

        logTransaction(item, TransactionType.INBOUND, (long) qty, "PUTAWAY");
    }

    private void syncBinMetrics(StorageBin bin, Product product, int qty, String warehouseId) {

        StorageBin locked = binRepository.findByIdWithLock(bin.getId(), warehouseId)
                .orElseThrow();

        BigDecimal deltaVol = product.getUnitVolume().multiply(BigDecimal.valueOf(qty));

        double newVolume = locked.getCurrentVolumeOccupied() + deltaVol.doubleValue();
        double newWeight = locked.getCurrentWeightLoad() + (product.getWeight() * qty);

        // Clamp to zero — floating-point drift can produce tiny negatives after repeated operations
        locked.setCurrentVolumeOccupied(Math.max(0.0, newVolume));
        locked.setCurrentWeightLoad(Math.max(0.0, newWeight));

        // Keep bin status in sync with actual occupancy
        if (locked.getCurrentVolumeOccupied() <= 0.0 && locked.getCurrentWeightLoad() <= 0.0) {
            locked.setStatus(BinStatus.AVAILABLE);
        } else if (locked.getCurrentVolumeOccupied() >= locked.getMaxVolume()
                || locked.getCurrentWeightLoad() >= locked.getMaxWeightCapacity()) {
            locked.setStatus(BinStatus.FULL);
        } else if (locked.getStatus() == BinStatus.FULL) {
            // Was full, now has space — mark available again
            locked.setStatus(BinStatus.AVAILABLE);
        }

        binRepository.save(locked);
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

    private String getWarehouseId() {
        String id = WarehouseContext.get();
        if (id == null) throw new IllegalStateException("Tenant missing");
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFullyPicked(String inventoryItemId) {
        String warehouseId = getWarehouseId();
        return inventoryRepository.findByIdWithLock(inventoryItemId, warehouseId)
                .map(item -> item.getReservedQuantity() == 0)
                .orElse(true); // if item not found, treat as picked (already committed)
    }
}