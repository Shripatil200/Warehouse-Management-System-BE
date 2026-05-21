package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ProductMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductMasterRepository extends JpaRepository<ProductMaster, String> {

    Optional<ProductMaster> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    @Query("SELECT pm FROM ProductMaster pm LEFT JOIN FETCH pm.category " +
            "WHERE (:query IS NULL OR LOWER(pm.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(pm.barcode) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ProductMaster> search(@Param("query") String query, Pageable pageable);
}