package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Aisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tenant-aware Aisle repository.
 */
@Repository
public interface AisleRepository extends JpaRepository<Aisle, String> {

    boolean existsByCodeAndZoneId(String code, String zoneId);

    // OPTIONAL (stronger validation)
    boolean existsByCodeAndZoneIdAndZone_Warehouse_Id(
            String code,
            String zoneId,
            String warehouseId
    );
}