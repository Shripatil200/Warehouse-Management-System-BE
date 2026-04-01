package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Supplier data.
 * Supports Week 2 requirements for identifying the source of incoming shipments.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    /**
     * Finds a supplier by name.
     * Useful for manual entry verification by Warehouse Managers.
     */
    Optional<Supplier> findByName(String name);

    /**
     * Checks if a supplier exists with a specific email.
     * Ensures data integrity in the supplier registry.
     */
    boolean existsByContactEmail(String contactEmail);
}