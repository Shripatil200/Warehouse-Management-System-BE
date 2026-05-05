package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.enums.BinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the Product Catalog and Stock Intelligence.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // --- Core Count & Uniqueness ---

    long countByWarehouseId(String warehouseId);

    boolean existsBySkuIgnoreCaseAndWarehouseId(String sku, String warehouseId);

    Optional<Product> findBySkuAndWarehouseIdAndActiveTrue(String sku, String warehouseId);

    Optional<Product> findByIdAndWarehouseId(String id, String warehouseId);

    // --- Scoped Pagination ---

    Page<Product> findAllByWarehouseIdAndActiveTrue(String warehouseId, Pageable pageable);

    Page<Product> findAllByWarehouseId(String warehouseId, Pageable pageable);

    // --- Stock Intelligence & Threshold Monitoring ---

    /**
     * Procurement Metric: Counts products requiring a Purchase Order (Global Shortage).
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    Long countGlobalLowStock(@Param("id") String warehouseId);

    /**
     * Operational Metric: Counts products needing movement from BULK to PICKING.
     */
    @Query("SELECT COUNT(DISTINCT p) FROM Product p JOIN p.inventoryItems i " +
            "WHERE p.warehouse.id = :id " +
            "AND i.storageBin.binType = :binType " +
            "AND i.quantity < p.minReplenishThreshold")
    Long countProductsNeedingReplenishment(
            @Param("id") String warehouseId,
            @Param("binType") BinType binType);

    /**
     * Generates a replenishment list for the internal movement queue.
     * UPDATED: Removed hardcoded Enum path to use :binType parameter.
     */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.inventoryItems i " +
            "WHERE p.warehouse.id = :id AND p.active = true " +
            "AND i.storageBin.binType = :binType " + // Refactored from hardcoded string
            "AND i.quantity < p.minReplenishThreshold")
    List<Product> findProductsNeedingReplenishment(
            @Param("id") String warehouseId,
            @Param("binType") BinType binType);

    /**
     * Procurement List: Products hitting safety stock levels.
     */
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    List<Product> findGlobalLowStockProducts(@Param("id") String warehouseId);

    // --- Capacity Analytics ---

    @Query("SELECT COALESCE(SUM(i.quantity), 0L) FROM InventoryItem i WHERE i.storageBin.aisle.zone.warehouse.id = :warehouseId")
    Long sumCurrentStockByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("SELECT COALESCE(SUM(b.currentVolumeOccupied), 0.0) FROM StorageBin b " +
            "WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Double sumCurrentOccupancyByWarehouseId(@Param("warehouseId") String warehouseId);
}