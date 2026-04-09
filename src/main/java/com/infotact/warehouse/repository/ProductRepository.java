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
 * <p>
 * This repository manages product master data and implements the core logic
 * for inventory health monitoring (Low Stock alerts) and warehouse capacity
 * utilization calculations.
 * </p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Prevents duplicate SKU entry regardless of letter casing.
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Retrieves an operational product by its SKU.
     */
    Optional<Product> findBySkuAndActiveTrue(String sku);

    /**
     * Provides a paginated view of all non-archived products.
     */
    Page<Product> findAllByActiveTrue(Pageable pageable);

    /**
     * Internal lookup including inactive products.
     */
    Optional<Product> findBySku(String sku);

    /**
     * Facility Metric: Total unique SKUs registered in a specific warehouse.
     */
    Long countByWarehouseId(String warehouseId);

    /**
     * THRESHOLD MONITOR: Counts products needing replenishment.
     * <p>
     * Logic: Identifies products where the aggregate quantity across all bins
     * is less than or equal to the defined {@code minThreshold}.
     * </p>
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    Long countLowStock(@Param("id") String warehouseId);

    /**
     * REPLENISHMENT LIST: Fetches full product details for low-stock items.
     * <p>
     * Usage: Primarily used to generate 'Reorder Reports' or automated
     * Purchase Order drafts.
     * </p>
     */
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    List<Product> findLowStockProducts(@Param("id") String warehouseId);

    /**
     * CAPACITY ANALYTICS: Total units currently in stock.
     * <p>
     * Logic: Traverses the hierarchy from Warehouse -> Zone -> Aisle -> Bin -> Item
     * to sum up every single unit physically present in the facility.
     * </p>
     */
    @Query("SELECT SUM(i.quantity) FROM InventoryItem i WHERE i.storageBin.aisle.zone.warehouse.id = :id")
    Long sumCurrentStockByWarehouseId(@Param("id") String warehouseId);

    /**
     * CAPACITY ANALYTICS: Total volume/slots occupied.
     * <p>
     * Logic: Sums the 'currentOccupancy' field across all bins in the warehouse hierarchy.
     * This is used to calculate the 'Fill Percentage' of the facility.
     * </p>
     */
    @Query("SELECT SUM(b.currentOccupancy) FROM StorageBin b WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Long sumCurrentOccupancyByWarehouseId(@Param("warehouseId") String warehouseId);
}