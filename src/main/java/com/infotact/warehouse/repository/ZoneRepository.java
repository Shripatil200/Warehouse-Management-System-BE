package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object for the physical 'Zone' infrastructure.
 * <p>
 * This repository manages the first level of physical segmentation within a facility.
 * It ensures that logical areas (e.g., 'Shipping', 'Receiving', 'Hazardous')
 * are correctly mapped and isolated at the warehouse level.
 * </p>
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, String> {

    /**
     * Checks if a zone name is already in use within a specific facility.
     * <p>
     * Logic: Zone names must be unique per {@link com.infotact.warehouse.entity.Warehouse}.
     * This prevents administrative errors while allowing different facilities
     * in the same system to use identical naming conventions.
     * </p>
     * @param name The descriptive name of the zone.
     * @param warehouseId The unique identifier of the parent facility.
     * @return true if a zone with this name exists in the specified warehouse.
     */
    boolean existsByNameAndWarehouseId(String name, String warehouseId);
}