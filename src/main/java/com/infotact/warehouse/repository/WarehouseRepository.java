package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Warehouse entity operations.
 * <p>
 * This repository manages structural data and high-performance aggregate metrics.
 * It uses JPQL with explicit JOIN FETCH and GROUP BY clauses to prevent
 * the N+1 select problem and Cartesian product bloat.
 * </p>
 *
 * @author Gemini
 * @version 3.1
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {

    /**
     * Optimized fetch for hierarchical layout structure.
     * <p>
     * Uses JOIN FETCH to initialize Zones and Aisles in a single database trip.
     * Note: Bins are excluded here to keep the memory footprint low during structural browsing.
     * </p>
     *
     * @param id The unique UUID of the warehouse.
     * @return Optional containing the Warehouse entity with pre-loaded structure.
     */
    @Query("""
        SELECT w FROM Warehouse w 
        LEFT JOIN FETCH w.zones z 
        LEFT JOIN FETCH z.aisles a 
        WHERE w.id = :id
    """)
    Optional<Warehouse> findOptimizedLayout(@Param("id") String id);

    /**
     * Single-query utilization calculator for the management dashboard.
     * <p>
     * Aggregates volumetric metrics across the entire facility.
     * Returns a List containing one Object array:
     * <ul>
     *     <li>Index 0: Total Volumetric Capacity (Double)</li>
     *     <li>Index 1: Total Current Occupancy (Double)</li>
     * </ul>
     * </p>
     *
     * @param warehouseId The warehouse UUID.
     * @return List of aggregated numeric results.
     */
    @Query("""
        SELECT COALESCE(SUM(b.maxVolume), 0.0), 
               COALESCE(SUM(b.currentVolumeOccupied), 0.0)
        FROM StorageBin b
        WHERE b.warehouse.id = :warehouseId
    """)
    List<Object[]> findWarehouseUtilization(@Param("warehouseId") String warehouseId);

    /**
     * Batch metric and inventory count calculation at the Aisle level.
     * <p>
     * <b>Update:</b> Added COUNT(b.id) to provide bin totals for UI badges.
     * Returns a list of Object arrays containing:
     * <ul>
     *     <li>Index 0: Aisle ID (String)</li>
     *     <li>Index 1: Total Volumetric Capacity (Double)</li>
     *     <li>Index 2: Current Occupancy (Double)</li>
     *     <li>Index 3: Total Bin Count (Long)</li>
     * </ul>
     * </p>
     *
     * @param warehouseId The unique identifier of the warehouse.
     * @return A list of aisle-level operational metrics.
     */
    @Query("""
        SELECT a.id, 
               COALESCE(SUM(b.maxVolume), 0.0), 
               COALESCE(SUM(b.currentVolumeOccupied), 0.0),
               COUNT(b.id) 
        FROM Aisle a 
        LEFT JOIN StorageBin b ON b.aisle.id = a.id
        WHERE a.zone.warehouse.id = :warehouseId 
        GROUP BY a.id
    """)
    List<Object[]> findAllAisleMetricsByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * Checks if a warehouse exists by name (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if a warehouse exists with a specific name and location combination.
     */
    boolean existsByNameIgnoreCaseAndLocationIgnoreCase(String name, String location);
}