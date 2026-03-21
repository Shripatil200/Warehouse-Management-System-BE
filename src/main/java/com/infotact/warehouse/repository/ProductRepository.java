package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    // Unique SKU check for Create/Update validation
    boolean existsBySkuIgnoreCase(String sku);

    // Fast lookup by SKU (Industry standard for mobile scanners) [cite: 27, 163]
    Optional<Product> findBySkuAndActiveTrue(String sku);

    // Standard pagination for active inventory [cite: 21, 161]
    Page<Product> findAllByActiveTrue(Pageable pageable);

    // Find all for Admin views [cite: 161]
    Page<Product> findAll(Pageable pageable);
}