package com.infotact.warehouse.repository;

import com.infotact.warehouse.dto.v1.response.InventorySummaryResponse;
import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.entity.enums.BinType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    // ============================================================
    // CORE LOOKUPS
    // ============================================================

    Optional<InventoryItem> findByProductIdAndStorageBinIdAndBatchNumberAndPurchasePriceAndExpiryDateAndStorageBin_Warehouse_Id(
            String productId,
            String binId,
            String batch,
            BigDecimal cost,
            LocalDate expiryDate,
            String warehouseId
    );

    List<InventoryItem> findAllByProductSkuAndStorageBin_Warehouse_Id(
            String sku,
            String warehouseId
    );

    List<InventoryItem> findAllByStorageBinIdAndStorageBin_Warehouse_Id(
            String binId,
            String warehouseId
    );

    // ============================================================
    // LOCKING (CRITICAL)
    // ============================================================

    // 🔒 Row-level lock for single item
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i FROM InventoryItem i
        WHERE i.id = :id
        AND i.storageBin.warehouse.id = :warehouseId
    """)
    Optional<InventoryItem> findByIdWithLock(
            @Param("id") String id,
            @Param("warehouseId") String warehouseId
    );

    //  Batch locking using SKIP LOCKED (PostgreSQL / Oracle)
    @Query(value = """
        SELECT * FROM inventory_item i
        WHERE i.status = :status
        AND i.expiry_date < :date
        AND i.warehouse_id = :warehouseId
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
    """, nativeQuery = true)
    List<InventoryItem> lockNextExpiredBatch(
            @Param("status") String status,
            @Param("date") LocalDate date,
            @Param("warehouseId") String warehouseId,
            @Param("limit") int limit
    );

    // ============================================================
    // PICKING (FEFO + LOCK READY)
    // ============================================================

    @Query("""
        SELECT i FROM InventoryItem i 
        WHERE i.product.id = :productId 
        AND i.status = :status 
        AND i.storageBin.binType = :binType 
        AND i.storageBin.warehouse.id = :warehouseId
        AND (i.quantity - i.reservedQuantity) > 0
        ORDER BY i.expiryDate ASC NULLS LAST, i.createdAt ASC
    """)
    List<InventoryItem> findAvailableStockForPicking(
            @Param("productId") String productId,
            @Param("status") InventoryStatus status,
            @Param("binType") BinType binType,
            @Param("warehouseId") String warehouseId
    );

    // ============================================================
    // BULK SOURCE (REPLENISHMENT)
    // ============================================================

    @Query("""
        SELECT i FROM InventoryItem i 
        WHERE i.product.id = :productId 
        AND i.status = :status 
        AND i.storageBin.binType = :binType 
        AND i.storageBin.warehouse.id = :warehouseId
        AND (i.quantity - i.reservedQuantity) > 0
        ORDER BY i.expiryDate ASC NULLS LAST, i.createdAt ASC
    """)
    List<InventoryItem> findBulkSourceForReplenishment(
            @Param("productId") String productId,
            @Param("status") InventoryStatus status,
            @Param("binType") BinType binType,
            @Param("warehouseId") String warehouseId
    );

    // ============================================================
    // EXPIRY (SCHEDULER SAFE)
    // ============================================================

    @Query("""
        SELECT i FROM InventoryItem i 
        WHERE i.status = :status 
        AND i.expiryDate < :date 
        AND i.storageBin.warehouse.id = :warehouseId
    """)
    Page<InventoryItem> findExpiredStockForWarehouse(
            @Param("status") InventoryStatus status,
            @Param("date") LocalDate date,
            @Param("warehouseId") String warehouseId,
            Pageable pageable
    );

    // ============================================================
    // SUMMARY
    // ============================================================

    @Query("""
        SELECT new com.infotact.warehouse.dto.v1.response.InventorySummaryResponse(
            i.product.sku,
            i.product.name,
            SUM(i.quantity),
            SUM(i.reservedQuantity),
            SUM(i.quantity - i.reservedQuantity),
            COUNT(DISTINCT i.storageBin.id),
            i.product.costPrice,
            i.product.sellingPrice,
            SUM(i.quantity * i.product.costPrice)
        )
        FROM InventoryItem i 
        WHERE i.storageBin.warehouse.id = :warehouseId
        GROUP BY i.product.sku, i.product.name, i.product.costPrice, i.product.sellingPrice
    """)
    Page<InventorySummaryResponse> findGlobalInventorySummaryByWarehouse(
            @Param("warehouseId") String warehouseId,
            Pageable pageable
    );

    // ============================================================
    // DETAIL VIEW
    // ============================================================

    @Query("""
        SELECT i FROM InventoryItem i 
        WHERE i.storageBin.warehouse.id = :warehouseId
        AND (:sku IS NULL OR i.product.sku = :sku)
        AND (:binCode IS NULL OR i.storageBin.binCode = :binCode)
    """)
    Page<InventoryItem> findDetailedInventory(
            @Param("warehouseId") String warehouseId,
            @Param("sku") String sku,
            @Param("binCode") String binCode,
            Pageable pageable
    );

    // ============================================================
    // LOW STOCK
    // ============================================================

    @Query("""
        SELECT new com.infotact.warehouse.dto.v1.response.InventorySummaryResponse(
            i.product.sku,
            i.product.name,
            SUM(i.quantity),
            SUM(i.reservedQuantity),
            SUM(i.quantity - i.reservedQuantity),
            COUNT(DISTINCT i.storageBin.id),
            i.product.costPrice,
            i.product.sellingPrice,
            SUM(i.quantity * i.product.costPrice)
        )
        FROM InventoryItem i 
        WHERE i.storageBin.warehouse.id = :warehouseId
        GROUP BY i.product.sku, i.product.name, i.product.costPrice, i.product.sellingPrice
        HAVING SUM(i.quantity - i.reservedQuantity) <= :threshold
    """)
    Page<InventorySummaryResponse> findLowStockSummaryByWarehouse(
            @Param("threshold") Integer threshold,
            @Param("warehouseId") String warehouseId,
            Pageable pageable
    );
}