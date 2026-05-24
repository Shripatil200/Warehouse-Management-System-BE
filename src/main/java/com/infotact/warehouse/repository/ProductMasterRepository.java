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

    @Query(value = "SELECT pm FROM ProductMaster pm LEFT JOIN FETCH pm.category",
           countQuery = "SELECT COUNT(pm) FROM ProductMaster pm")
    Page<ProductMaster> findAllWithCategory(Pageable pageable);

    @Query(value = "SELECT pm FROM ProductMaster pm LEFT JOIN FETCH pm.category " +
            "WHERE LOWER(pm.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(pm.barcode) LIKE LOWER(CONCAT('%', :query, '%'))",
           countQuery = "SELECT COUNT(pm) FROM ProductMaster pm " +
            "WHERE LOWER(pm.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(pm.barcode) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ProductMaster> search(@Param("query") String query, Pageable pageable);
}