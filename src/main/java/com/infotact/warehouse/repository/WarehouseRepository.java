package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {

    @Query("SELECT COALESCE(SUM(b.maxVolume), 0.0) FROM StorageBin b " +
            "WHERE b.aisle.zone.warehouse.id = :warehouseId")
    Double findTotalCapacityByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("SELECT w FROM Warehouse w LEFT JOIN FETCH w.zones WHERE w.id = :id")
    Optional<Warehouse> findByIdWithZones(@Param("id") String id);

    /**
     * UPDATED: Removed CAST and changed return type to Double
     */
    @Query("SELECT COALESCE(SUM(b.maxVolume), 0.0) FROM StorageBin b WHERE b.aisle.zone.id = :zoneId")
    Double sumCapacityByZoneId(@Param("zoneId") String zoneId);

    @Query("SELECT COALESCE(SUM(b.currentVolumeOccupied), 0.0) FROM StorageBin b WHERE b.aisle.zone.id = :zoneId")
    Double sumOccupancyByZoneId(@Param("zoneId") String zoneId);

    @Query("SELECT COALESCE(SUM(b.maxVolume), 0.0) FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Double sumCapacityByAisleId(@Param("aisleId") String aisleId);

    @Query("SELECT COALESCE(SUM(b.currentVolumeOccupied), 0.0) FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Double sumOccupancyByAisleId(@Param("aisleId") String aisleId);

    boolean existsByNameIgnoreCaseAndLocationIgnoreCase(String name, String location);
    boolean existsByNameIgnoreCase(String name);
}