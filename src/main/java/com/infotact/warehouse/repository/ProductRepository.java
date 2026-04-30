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
 * This repository manages the master product list and provides critical analytics
 * for warehouse operations, including low-stock monitoring and volumetric occupancy.
 * </p>
 * <p>
 * <b>Security & Isolation:</b> All methods are designed to respect warehouse-level
 * multi-tenancy. Uniqueness checks and lookups are scoped to a specific facility
 * to support the "One Warehouse per Admin" model.
 * </p>
 *
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // --- Scoped Uniqueness & Lookups ---

    /**
     * Verifies if a SKU exists within a specific warehouse.
     * <p>
     * Logic: Prevents duplicate SKU entries within the same facility while allowing
     * the same SKU to exist in different independent warehouses.
     * </p>
     * @param sku The Stock Keeping Unit to check.
     * @param warehouseId The UUID of the facility context.
     * @return true if the SKU is already registered in this warehouse.
     */
    boolean existsBySkuIgnoreCaseAndWarehouseId(String sku, String warehouseId);

    /**
     * Retrieves an active product by its SKU within a specific warehouse.
     * @param sku The business-level SKU.
     * @param warehouseId The UUID of the facility context.
     * @return An Optional containing the active product if found.
     */
    Optional<Product> findBySkuAndWarehouseIdAndActiveTrue(String sku, String warehouseId);

    /**
     * Retrieves a product by ID only if it belongs to the specified warehouse.
     * @param id The UUID of the product.
     * @param warehouseId The UUID of the facility context.
     * @return An Optional containing the product.
     */
    Optional<Product> findByIdAndWarehouseId(String id, String warehouseId);

    // --- Scoped Pagination ---

    /**
     * Provides a paginated view of all non-archived products for a specific warehouse.
     * @param warehouseId The facility identifier.
     * @param pageable Pagination and sorting parameters.
     * @return A page of active products belonging to the warehouse.
     */
    Page<Product> findAllByWarehouseIdAndActiveTrue(String warehouseId, Pageable pageable);

    /**
     * Provides a paginated view of all products (including inactive) for a specific warehouse.
     * @param warehouseId The facility identifier.
     * @param pageable Pagination and sorting parameters.
     * @return A page of all products belonging to the warehouse.
     */
    Page<Product> findAllByWarehouseId(String warehouseId, Pageable pageable);

    // --- Stock Intelligence & Threshold Monitoring ---

    /**
     * Counts products requiring replenishment within a facility.
     * <p>
     * <b>Logic:</b> Sums current quantities from {@code InventoryItem} and compares
     * against the product's {@code minThreshold}.
     * </p>
     * @param warehouseId The UUID of the warehouse.
     * @return Count of active products currently at or below their safety stock level.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    Long countLowStock(@Param("id") String warehouseId);

    /**
     * Generates a replenishment list for a specific warehouse.
     * @param warehouseId The UUID of the warehouse.
     * @return List of Product entities that have hit their safety stock threshold.
     */
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :id AND p.active = true AND p.id IN (" +
            "SELECT i.product.id FROM InventoryItem i GROUP BY i.product.id HAVING SUM(i.quantity) <= p.minThreshold)")
    List<Product> findLowStockProducts(@Param("id") String warehouseId);

    // --- Capacity Analytics ---

    /**
     * Facility Metric: Total physical units currently stored in the warehouse.
     * @param warehouseId The UUID of the warehouse.
     * @return Sum of all quantities across all bins.
     */
    @Query("SELECT COALESCE(SUM(i.quantity), 0L) FROM InventoryItem i WHERE i.storageBin.aisle.zone.warehouse.id = :warehouseId")
    Long sumCurrentStockByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * Total volumetric occupancy analysis.
     * <p>
     * <b>Unit:</b> Cubic Centimeters (cm³).
     * <b>Logic:</b> Aggregates the {@code currentVolumeOccupied} from all storage bins
     * within the facility hierarchy.
     * </p>
     * @param warehouseId The UUID of the warehouse.
     * @return Total volume currently occupied as a Double.
     */
    @Query("SELECT COALESCE(SUM(b.currentVolumeOccupied), 0.0) FROM StorageBin b " +
            "WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Double sumCurrentOccupancyByWarehouseId(@Param("warehouseId") String warehouseId);
}