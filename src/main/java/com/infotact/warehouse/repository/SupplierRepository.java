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

    Page<Supplier> findByStatus(SupplierStatus status, Pageable pageable);

    Page<Supplier> findAll(Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Supplier> search(@Param("q") String query, Pageable pageable);
}
