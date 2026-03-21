package com.infotact.warehouse.repository;


import com.infotact.warehouse.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

    boolean existsByNameIgnoreCase(String name);

    // Filter for standard users [cite: 161]
    Page<ProductCategory> findAllByActiveTrue(Pageable pageable);

    // Standard lookup for single active entity
    Optional<ProductCategory> findByIdAndActiveTrue(String id);
}