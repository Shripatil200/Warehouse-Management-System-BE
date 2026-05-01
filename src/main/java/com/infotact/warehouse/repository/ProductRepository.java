package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Product;
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
 *
 * Performance Features:
 * - Scoped Queries: Optimized for high-speed retrieval using indexed warehouse identifiers.
 * - Multi-Tenancy: Enforces isolation as per the "One Warehouse per Admin" rule.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // --- Core Count & Uniqueness ---

    /**
     * Counts all products registered to a specific warehouse.
     * Required for top-level dashboard metrics.
     *
     * @param warehouseId The UUID of the facility context.
     * @return Total count of products in the warehouse.
     */
    long countByWarehouseId(String warehouseId);

    /**
     * Verifies if a SKU exists within a specific warehouse context.
     */
    boolean existsBySkuIgnoreCaseAndWarehouseId(String sku, String warehouseId);

    /**
     * Retrieves an active product by its SKU within a specific warehouse.
     */
    Optional<Product> findBySkuAndWarehouseIdAndActiveTrue(String sku, String warehouseId);

    /**
     * Retrieves a product by ID only if it belongs to the specified warehouse.
     */
    Optional<Product> findByIdAndWarehouseId(String id, String warehouseId);

    // --- Scoped Pagination ---

    /**
     * Provides a paginated view of all active products for a specific warehouse.
     */
    Page<Product> findAllByWarehouseIdAndActiveTrue(String warehouseId, Pageable pageable);

    /**
     * Provides a paginated view of all products (including inactive) for a specific warehouse.
     */
    Page<Product> findAllByWarehouseId(String warehouseId, Pageable pageable);

    // --- Stock Intelligence & Threshold Monitoring ---

    /**
     * Counts products requiring replenishment within a facility.
     * Logic: Compares total inventory quantity against product safety thresholds.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    Long countLowStock(@Param("id") String warehouseId);

    /**
     * Generates a replenishment list of products that have hit their safety stock level.
     */
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    List<Product> findLowStockProducts(@Param("id") String warehouseId);

    // --- Capacity Analytics (Used by Movement & Inventory Services) ---

    /**
     * Facility Metric: Total physical units currently stored in the warehouse.
     */
    @Query("SELECT COALESCE(SUM(i.quantity), 0L) FROM InventoryItem i WHERE i.storageBin.aisle.zone.warehouse.id = :warehouseId")
    Long sumCurrentStockByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * Total volumetric occupancy analysis in Cubic Centimeters (cm³).
     * Logic: Aggregates real-time occupancy from all storage bins.
     */
    @Query("SELECT COALESCE(SUM(b.currentVolumeOccupied), 0.0) FROM StorageBin b " +
            "WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Double sumCurrentOccupancyByWarehouseId(@Param("warehouseId") String warehouseId);
}