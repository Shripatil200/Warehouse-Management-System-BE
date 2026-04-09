package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'InventoryItem' transactions.
 * <p>
 * This is the most sensitive repository in the system, handling the real-time
 * stock levels of products across various bins. It implements row-level locking
 * to ensure atomicity during high-concurrency operations like Picking and Putaway.
 * </p>
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Locates a specific product record within a specific storage bin.
     * @param sku The Stock Keeping Unit of the product.
     * @param binId The unique identifier of the target bin.
     */
    Optional<InventoryItem> findByProductSkuAndStorageBinId(String sku, String binId);

    /**
     * CRITICAL TRANSACTIONAL LOCK: Pessimistic Write.
     * <p>
     * Logic: This method locks the specific inventory row at the database level.
     * Use this during stock deductions (Picking) or additions (Putaway) to prevent
     * Race Conditions where two threads might attempt to update the same
     * inventory count at the same time.
     * </p>
     * @param sku The product SKU.
     * @param binId The bin UUID.
     * @return The inventory item, exclusively locked for the current transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.product.sku = :sku AND i.storageBin.id = :binId")
    Optional<InventoryItem> findBySkuAndBinWithLock(String sku, String binId);

    /**
     * Retrieves all physical locations (bins) where a specific SKU is currently stored.
     * <p>
     * Usage: Essential for the 'Picking Engine' to suggest the best paths
     * for a worker to collect stock.
     * </p>
     */
    List<InventoryItem> findAllByProductSku(String sku);

    /**
     * Audit Query: Lists every product currently residing in a specific bin.
     */
    List<InventoryItem> findAllByStorageBinId(String binId);

    /**
     * Standard lookup by Internal Product ID and Bin ID.
     */
    Optional<InventoryItem> findByProductIdAndStorageBinId(String id, String id1);
}