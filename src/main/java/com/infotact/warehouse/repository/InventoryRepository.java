package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.entity.enums.BinType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Locates a specific inventory layer based on cost and batch.
     */
    Optional<InventoryItem> findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePrice(
            String productId, String binId, String batch, BigDecimal price);

    /**
     * CRITICAL TRANSACTIONAL LOCK: Pessimistic Write to prevent "lost updates".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithLock(@Param("id") String id);

    /**
     * Retrieves all storage locations for a specific product SKU.
     */
    List<InventoryItem> findAllByProductSku(String sku);

    /**
     * Lists all products currently residing within a specific storage bin.
     */
    List<InventoryItem> findAllByStorageBinId(String binId);

    Optional<InventoryItem> findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDate(
            String productId, String binId, String batch, BigDecimal cost, LocalDate expiryDate);

    /**
     * REFINED PICKING QUERY: Only finds stock in PICK_FACE bins.
     * Prioritized by Expiry Date (FEFO) then Created Date (FIFO).
     *
     * UPDATED: Uses parameters for Enums and 'createdAt' to fix Hibernate resolution errors.
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.product.id = :productId " +
            "AND i.status = :status " +
            "AND i.storageBin.binType = :binType " +
            "AND i.quantity > i.reservedQuantity " +
            "ORDER BY i.expiryDate ASC, i.createdAt ASC")
    List<InventoryItem> findAvailableStockForPicking(
            @Param("productId") String productId,
            @Param("status") InventoryStatus status,
            @Param("binType") BinType binType
    );

    /**
     * REPLENISHMENT SOURCE QUERY: Finds stock specifically in BULK_STORAGE.
     * Used when the picking bins are low and need a refill from the racks.
     *
     * UPDATED: Uses parameters for Enums and 'createdAt' to fix Hibernate resolution errors.
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.product.id = :productId " +
            "AND i.status = :status " +
            "AND i.storageBin.binType = :binType " +
            "AND i.quantity > i.reservedQuantity " +
            "ORDER BY i.expiryDate ASC, i.createdAt ASC")
    List<InventoryItem> findBulkSourceForReplenishment(
            @Param("productId") String productId,
            @Param("status") InventoryStatus status,
            @Param("binType") BinType binType
    );

    /**
     * Find items that have expired for automated quarantine tasks.
     */
    List<InventoryItem> findAllByStatusAndExpiryDateBefore(InventoryStatus status, LocalDate date);
}