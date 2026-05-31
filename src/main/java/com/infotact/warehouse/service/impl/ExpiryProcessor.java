package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.*;
import com.infotact.warehouse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class ExpiryProcessor {

    private final InventoryRepository inventoryRepository;
    private final BinRepository binRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(String itemId, String warehouseId) {

        InventoryItem item = inventoryRepository
                .findByIdWithLock(itemId, warehouseId)
                .orElse(null);

        if (item == null) return;

        // ✅ Idempotent check
        if (item.getStatus() != InventoryStatus.AVAILABLE)
            return;

        item.setStatus(InventoryStatus.EXPIRED);
        inventoryRepository.save(item);

        syncBin(item, warehouseId);
        logTransaction(item);
    }

    private void syncBin(InventoryItem item, String warehouseId) {

        StorageBin bin = binRepository.findByIdWithLock(
                item.getStorageBin().getId(),
                warehouseId
        ).orElseThrow();

        double volume =
                item.getProduct().getUnitVolume().doubleValue() * item.getQuantity();

        double weight =
                item.getProduct().getWeight() * item.getQuantity();

        bin.setCurrentVolumeOccupied(
                Math.max(0, bin.getCurrentVolumeOccupied() - volume)
        );

        bin.setCurrentWeightLoad(
                Math.max(0, bin.getCurrentWeightLoad() - weight)
        );

        binRepository.save(bin);
    }

    private void logTransaction(InventoryItem item) {

        InventoryTransaction tx = new InventoryTransaction();

        tx.setInventoryItem(item);
        tx.setWarehouse(item.getWarehouse());
        tx.setType(TransactionType.ADJUSTMENT);
        tx.setQuantityChange((long) -item.getQuantity());
        tx.setUnitPrice(item.getPurchasePrice());
        tx.setReasonCode("AUTO_EXPIRED");

        transactionRepository.save(tx);
    }
}