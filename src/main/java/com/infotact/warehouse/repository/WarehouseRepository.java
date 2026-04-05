package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {

    /**
     * Used for validating unique warehouse names during creation/update.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Standard view for operational staff - only shows warehouses that aren't soft-deleted.
     */
    Page<Warehouse> findAllByActiveTrue(Pageable pageable);

    /**
     * Standard view for admins - allows toggling between active and inactive facilities.
     */
    Page<Warehouse> findAllByActive(boolean active, Pageable pageable);

    /**
     * Finds a specific active warehouse.
     * Essential for ensuring stock isn't received into a deactivated facility.
     */
    Optional<Warehouse> findByIdAndActiveTrue(String id);

    Optional<Warehouse> findByName(String systemHeadquarters);
}
