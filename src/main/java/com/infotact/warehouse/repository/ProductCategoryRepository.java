package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Object for Product Category management.
 * <p>
 * This repository manages the hierarchical classification system for products.
 * It enforces strict data isolation by anchoring category trees to specific
 * warehouses, ensuring each facility has its own customized catalog structure.
 * </p>
 */

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

    /**
     * Standard check for name uniqueness.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * RESOLVED METHOD:
     * This matches the naming convention [Property] + [Condition] + [Link] + [Property].
     * Logic: WHERE warehouse_id = ?1 AND active = true
     */
    Page<ProductCategory> findAllByWarehouseIdAndActiveTrue(String warehouseId, Pageable pageable);

    /**
     * Retrieves all categories for a warehouse, including inactive ones.
     */
    Page<ProductCategory> findAllByWarehouseId(String warehouseId, Pageable pageable);

    /**
     * Standard lookup that verifies the category is currently operational.
     */
    Optional<ProductCategory> findByIdAndActiveTrue(String id);
}