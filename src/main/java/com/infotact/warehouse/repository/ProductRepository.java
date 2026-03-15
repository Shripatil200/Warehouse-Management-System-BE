package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsBySkuIgnoreCase(String sku);
}