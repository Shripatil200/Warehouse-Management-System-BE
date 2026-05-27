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
 * It enforces strict data isolation by anchoring category trees to this warehouse.
 * </p>
 */
@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

    // CHANGE: Check if name exists ONLY within the specific warehouse
    boolean existsByNameIgnoreCaseAndWarehouseId(String name, String warehouseId);

    Page<ProductCategory> findAllByWarehouseIdAndActiveTrue(String warehouseId, Pageable pageable);

    Page<ProductCategory> findAllByWarehouseId(String warehouseId, Pageable pageable);

    // CHANGE: Ensure lookup by ID also respects the warehouse boundary
    Optional<ProductCategory> findByIdAndWarehouseId(String id, String warehouseId);

    Optional<ProductCategory> findByIdAndActiveTrue(String id);
}