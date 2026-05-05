package com.infotact.warehouse.repository;

import com.infotact.warehouse.dto.v1.response.FinancialMetricResponse;
import com.infotact.warehouse.entity.InventoryTransaction;
import com.infotact.warehouse.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for the immutable inventory ledger.
 * Handles the historical record of all stock movements and adjustments.
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {

    /**
     * Standard finder for a specific inventory item's history.
     * Updated: Changed 'CreatedAt' to 'TransactionDate' to match entity.
     */
    List<InventoryTransaction> findAllByInventoryItemIdOrderByTransactionDateDesc(String inventoryItemId);

    /**
     * Advanced Filtered Query for Management Reports.
     * Joins Transaction -> InventoryItem -> Product to allow filtering by SKU.
     */
    @Query("SELECT t FROM InventoryTransaction t " +
            "JOIN t.inventoryItem item " +
            "JOIN item.product p " +
            "WHERE (:sku IS NULL OR p.sku = :sku) " +
            "AND (:type IS NULL OR t.type = :type)")
    Page<InventoryTransaction> findFilteredTransactions(
            @Param("sku") String sku,
            @Param("type") TransactionType type,
            Pageable pageable);

    /**
     * Audit Query: Finds all actions that occurred in a specific bin location.
     */
    @Query("SELECT t FROM InventoryTransaction t " +
            "JOIN t.inventoryItem item " +
            "JOIN item.storageBin bin " +
            "WHERE bin.binCode = :binCode")
    Page<InventoryTransaction> findByBinCode(@Param("binCode") String binCode, Pageable pageable);

    /**
     * Date-range Query: For generating daily or weekly movement reports.
     * Updated: Changed 'createdAt' to 'transactionDate' to match entity.
     */
    @Query("SELECT t FROM InventoryTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    Page<InventoryTransaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT new com.infotact.warehouse.dto.v1.response.FinancialMetricResponse(" +
            "CAST(FUNCTION('DATE_FORMAT', t.transactionDate, :format) AS string), " +
            "SUM(CAST(t.quantityChange * t.unitPrice AS double)), " +
            "SUM(CAST(ABS(t.quantityChange) * t.unitPrice AS double)), " +
            "SUM(CAST(CASE WHEN t.type = 'ADJUSTMENT' AND t.quantityChange < 0 THEN (ABS(t.quantityChange) * t.unitPrice) ELSE 0 END AS double)), " +
            "SUM(CAST(CASE WHEN t.type = 'OUTBOUND' THEN (ABS(t.quantityChange) * (t.unitPrice - item.purchasePrice)) ELSE 0 END AS double))) " +
            "FROM InventoryTransaction t JOIN t.inventoryItem item " +
            "WHERE item.storageBin.warehouse.id = :warehouseId " +
            "AND (:productId IS NULL OR item.product.id = :productId) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, :format) " +
            "ORDER BY MIN(t.transactionDate) ASC")
    List<FinancialMetricResponse> getFinancialAnalytics(
            @Param("warehouseId") String warehouseId,
            @Param("productId") String productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("format") String format);
}