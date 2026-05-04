    package com.infotact.warehouse.repository;

    import com.infotact.warehouse.entity.InventoryItem;
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

    /**
     * Data Access Object for 'InventoryItem' transactions.
     * <p>
     * This repository handles real-time stock levels. It implements Row-Level Locking
     * (Pessimistic Write) to prevent race conditions during high-concurrency
     * Putaway and Picking operations.
     * </p>
     */
    @Repository
    public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

        /**
         * Locates a specific inventory layer based on cost and batch.
         * Essential for maintaining financial accuracy (valuation).
         *
         * @param productId The UUID of the product.
         * @param binId     The UUID of the storage bin.
         * @param batch     The batch/lot number.
         * @param price     The specific purchase cost for this stock layer.
         * @return An Optional containing the matching inventory record.
         */
        Optional<InventoryItem> findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePrice(
                String productId, String binId, String batch, BigDecimal price);

        /**
         * CRITICAL TRANSACTIONAL LOCK: Pessimistic Write.
         * <p>
         * Locks the specific inventory record at the database level until the current
         * transaction commits or rolls back. Use this for adjustments or picking
         * to prevent "lost updates" in high-traffic environments.
         * </p>
         *
         * @param id The internal UUID of the inventory item.
         * @return The inventory item, exclusively locked.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
        Optional<InventoryItem> findByIdWithLock(@Param("id") String id);

        /**
         * Retrieves all storage locations for a specific product SKU.
         * Used by the Picking Engine to calculate optimal travel paths.
         */
        List<InventoryItem> findAllByProductSku(String sku);

        /**
         * Lists all products currently residing within a specific storage bin.
         */
        List<InventoryItem> findAllByStorageBinId(String binId);

        Optional<InventoryItem> findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDate(String id, String id1, String batch, BigDecimal cost, LocalDate expiryDate);

        /**
         * Finds available stock for a product, prioritized by Expiry Date (FEFO).
         * Only considers items with 'AVAILABLE' status where physical qty > reserved qty.
         */
        @Query("SELECT i FROM InventoryItem i WHERE i.product.id = :productId " +
                "AND i.status = com.infotact.warehouse.entity.enums.InventoryStatus.AVAILABLE " +
                "AND i.quantity > i.reservedQuantity " +
                "ORDER BY i.expiryDate ASC, i.createdDate ASC")
        List<InventoryItem> findAvailableStockByProductFEFO(@Param("productId") String productId);
    }