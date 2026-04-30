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
 * <b>Update (v2.2):</b> Synchronized with the Volumetric & Weight-based capacity model.
 * All capacity analytics now target 'currentVolumeOccupied' and return Double
 * to prevent precision loss during dashboard calculations.
 * </p>
 *
 * @author Gemini
 * @version 2.2
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Prevents duplicate SKU entry regardless of letter casing.
     * @param sku The Stock Keeping Unit to check.
     * @return true if the SKU already exists.
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Retrieves an operational product by its SKU.
     * @param sku The product SKU.
     * @return An Optional containing the active product if found.
     */
    Optional<Product> findBySkuAndActiveTrue(String sku);

    /**
     * Provides a paginated view of all non-archived products.
     * @param pageable Pagination and sorting information.
     * @return A page of active products.
     */
    Page<Product> findAllByActiveTrue(Pageable pageable);

    /**
     * Internal lookup including inactive products.
     * @param sku The product SKU.
     * @return An Optional containing the product.
     */
    Optional<Product> findBySku(String sku);

    /**
     * Facility Metric: Total unique SKUs registered in a specific warehouse.
     * @param warehouseId The UUID of the warehouse.
     * @return Count of distinct product entries for this facility.
     */
    Long countByWarehouseId(String warehouseId);

    /**
     * THRESHOLD MONITOR: Counts products needing replenishment.
     * <p>
     * <b>Logic:</b> Identifies products where the aggregate quantity across all bins
     * is less than or equal to the defined {@code minThreshold}.
     * </p>
     * @param warehouseId The UUID of the warehouse.
     * @return Count of products currently in low-stock status.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    Long countLowStock(@Param("id") String warehouseId);

    /**
     * REPLENISHMENT LIST: Fetches full product details for low-stock items.
     * <p>
     * <b>Usage:</b> Primarily used to generate 'Reorder Reports' or automated
     * Purchase Order drafts.
     * </p>
     * @param warehouseId The UUID of the warehouse.
     * @return List of Product entities that have hit or fallen below their threshold.
     */
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    List<Product> findLowStockProducts(@Param("id") String warehouseId);

    /**
     * CAPACITY ANALYTICS: Total units currently in stock.
     * <p>
     * <b>Logic:</b> Traverses the hierarchy from Warehouse -> Zone -> Aisle -> Bin -> Item
     * to sum up every single unit physically present in the facility.
     * </p>
     * @param id The UUID of the warehouse.
     * @return Total quantity of all units (Integer sum).
     */
    @Query("SELECT COALESCE(SUM(i.quantity), 0L) FROM InventoryItem i WHERE i.storageBin.aisle.zone.warehouse.id = :id")
    Long sumCurrentStockByWarehouseId(@Param("id") String id);

    /**
     * CAPACITY ANALYTICS: Total volumetric occupancy (cm³).
     * <p>
     * <b>Logic:</b> Sums the 'currentVolumeOccupied' field across all storage bins
     * in the warehouse hierarchy.
     * </p>
     * <p>
     * <b>Critical Fix:</b> Updated attribute name from 'currentOccupancy' to
     * 'currentVolumeOccupied' and changed return type to Double to prevent
     * UnsatisfiedDependencyException during Spring Boot startup.
     * </p>
     * @param warehouseId The UUID of the warehouse.
     * @return Total volume currently occupied in cubic centimeters (cm³).
     */
    @Query("SELECT COALESCE(SUM(b.currentVolumeOccupied), 0.0) FROM StorageBin b " +
            "WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Double sumCurrentOccupancyByWarehouseId(@Param("warehouseId") String warehouseId);
}