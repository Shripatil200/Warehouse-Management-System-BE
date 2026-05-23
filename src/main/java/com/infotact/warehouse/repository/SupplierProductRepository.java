package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, String> {

    List<SupplierProduct> findByProductMasterIdAndActiveTrue(String productMasterId);

    List<SupplierProduct> findBySupplierIdAndActiveTrue(String supplierId);

    Optional<SupplierProduct> findByProductMasterIdAndSupplierId(String productMasterId, String supplierId);

    boolean existsByProductMasterIdAndSupplierId(String productMasterId, String supplierId);

    @Query("SELECT sp FROM SupplierProduct sp JOIN FETCH sp.supplier JOIN FETCH sp.productMaster " +
            "WHERE sp.productMaster.id = :productMasterId AND sp.active = true " +
            "ORDER BY sp.supplyPrice ASC")
    List<SupplierProduct> findAllByProductMasterOrderedByPrice(@Param("productMasterId") String productMasterId);
}
