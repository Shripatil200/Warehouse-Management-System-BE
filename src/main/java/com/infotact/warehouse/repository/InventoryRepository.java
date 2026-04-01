package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    // 1. Find a specific product in a specific bin
    Optional<InventoryItem> findByProductSkuAndStorageBinId(String sku, String binId);

    // 2. Lock the row for updates (Prevents race conditions)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.product.sku = :sku AND i.storageBin.id = :binId")
    Optional<InventoryItem> findBySkuAndBinWithLock(String sku, String binId);

    // 3. Find all locations for a product
    List<InventoryItem> findAllByProductSku(String sku);

    // 4. Find all items in a specific bin
    List<InventoryItem> findAllByStorageBinId(String binId);

    Optional<InventoryItem> findByProductIdAndStorageBinId(String id, String id1);
}