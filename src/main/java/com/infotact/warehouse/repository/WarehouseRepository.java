package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Object for the Warehouse Root Entity.
 * <p>
 * As the primary anchor for all multi-tenant data, this repository manages
 * facility lifecycle states and global capacity metrics. All physical layout
 * calculations (Zones, Aisles, Bins) originate from the identifiers managed here.
 * </p>
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {


    /**
     * Aggregates total capacity by traversing the hierarchy:
     * Warehouse -> Zone -> Aisle -> StorageBin.
     * * @param warehouseId The UUID of the facility.
     * @return Total capacity as a Double (sum of all bin capacities).
     */
    @Query("SELECT COALESCE(SUM(b.capacity), 0.0) FROM StorageBin b " +
            "WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Double findTotalCapacityByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * Optimized fetch for the Warehouse and its immediate Zones.
     * We avoid fetching Aisles/Bins here to keep the query light.
     */
    @Query("SELECT w FROM Warehouse w LEFT JOIN FETCH w.zones WHERE w.id = :id")
    Optional<Warehouse> findByIdWithZones(@Param("id") String id);

    /**
     * Computes the total capacity of a Zone by summing its child bins.
     */
    @Query("SELECT COALESCE(SUM(b.capacity), 0) FROM StorageBin b WHERE b.aisle.zone.id = :zoneId")
    Integer sumCapacityByZoneId(@Param("zoneId") String zoneId);

    /**
     * Computes the current occupancy of a Zone by summing its child bins.
     */
    @Query("SELECT COALESCE(SUM(b.currentOccupancy), 0) FROM StorageBin b WHERE b.aisle.zone.id = :zoneId")
    Integer sumOccupancyByZoneId(@Param("zoneId") String zoneId);

    /**
     * Computes the total capacity of an Aisle.
     */
    @Query("SELECT COALESCE(SUM(b.capacity), 0) FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Integer sumCapacityByAisleId(@Param("aisleId") String aisleId);

    /**
     * Computes the current occupancy of an Aisle.
     */
    @Query("SELECT COALESCE(SUM(b.currentOccupancy), 0) FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Integer sumOccupancyByAisleId(@Param("aisleId") String aisleId);

    /**
     * Checks if a warehouse exists with the given name, ignoring case sensitivity.
     * Spring Data JPA will automatically generate the implementation.
     */
    boolean existsByNameIgnoreCase(String name);


}