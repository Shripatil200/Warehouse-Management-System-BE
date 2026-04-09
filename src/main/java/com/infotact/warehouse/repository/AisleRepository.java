package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Aisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object for the physical 'Aisle' infrastructure.
 * <p>
 * This repository manages the second layer of the warehouse hierarchy.
 * All existence checks and lookups are scoped to specific Zones to allow
 * for localized naming conventions within different areas of the facility.
 * </p>
 */
@Repository
public interface AisleRepository extends JpaRepository<Aisle, String> {

    /**
     * Checks if an aisle code is already in use within a specific Zone.
     * <p>
     * Logic: Aisle codes (e.g., "A1") are unique per Zone. This allows different
     * zones (e.g., 'Cold Storage' and 'Bulk Storage') to potentially use
     * overlapping naming schemes if required by facility management.
     * </p>
     * * @param code   The alphanumeric identifier for the aisle.
     * @param zoneId The unique identifier of the parent zone.
     * @return true if the aisle code exists within the specified zone.
     */
    boolean existsByCodeAndZoneId(String code, String zoneId);
}