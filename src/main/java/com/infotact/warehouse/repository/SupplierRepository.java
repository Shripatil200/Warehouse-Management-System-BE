package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    Optional<Supplier> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    // Warehouse-scoped lookup by status
    Page<Supplier> findByStatusAndWarehouseId(SupplierStatus status, String warehouseId, Pageable pageable);

    // Warehouse-scoped list of all suppliers
    Page<Supplier> findAllByWarehouseId(String warehouseId, Pageable pageable);

    // Warehouse-scoped search by name, company, or email
    @Query("SELECT s FROM Supplier s WHERE s.warehouse.id = :warehouseId AND (" +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Supplier> search(@Param("q") String query, @Param("warehouseId") String warehouseId, Pageable pageable);
}