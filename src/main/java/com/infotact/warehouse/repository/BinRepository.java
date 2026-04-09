package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.StorageBin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data Access Object for the physical 'StorageBin' infrastructure.
 * <p>
 * This repository manages the final node of the warehouse hierarchy. It includes
 * high-performance projection queries and the 'Smart Putaway' algorithm used to
 * optimize storage efficiency and worker travel time.
 * </p>
 */
@Repository
public interface BinRepository extends JpaRepository<StorageBin, String> {

    /**
     * Retrieves a paginated list of bins within a specific aisle.
     * <p>
     * Optimization: Uses pagination to prevent memory overhead when rendering
     * large warehouse maps in the UI.
     * </p>
     */
    Page<StorageBin> findByAisleId(String aisleId, Pageable pageable);

    /**
     * Global unique check for a bin code (e.g., "A1-S1-01").
     */
    boolean existsByBinCodeIgnoreCase(String binCode);

    /**
     * Validates a bin's existence specifically within its assigned aisle.
     */
    Optional<StorageBin> findByBinCodeAndAisleId(String binCode, String aisleId);

    /**
     * Performance-optimized projection to retrieve only the unique codes in an aisle.
     * <p>
     * Usage: Ideal for quick validation checks or populating dropdowns
     * without loading full bin objects into memory.
     * </p>
     */
    @Query("SELECT b.binCode FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Set<String> findAllBinCodesByAisleId(@Param("aisleId") String aisleId);

    boolean existsByBinCode(String generatedCode);

    /**
     * THE SMART PUTAWAY ENGINE
     * <p>
     * Implements an intelligent suggestion algorithm for receiving stock:
     * 1. <b>Product Affinity:</b> Prioritizes bins that already contain the same Product
     * to keep stock grouped.
     * 2. <b>Consolidation Strategy:</b> Among candidate bins, prioritizes those with
     * the <i>least</i> remaining space to maximize overall warehouse density.
     * 3. <b>Constraints:</b> Only selects active, available bins with sufficient
     * remaining capacity.
     * </p>
     * @param productId The ID of the item being received.
     * @param zoneId Optional filter to restrict storage to a specific zone (e.g., Cold Storage).
     * @param requiredSpace The unit count to be stored.
     * @return A sorted list of ideal bins for placement.
     */
    @Query("SELECT b FROM StorageBin b " +
            "LEFT JOIN InventoryItem ii ON ii.storageBin.id = b.id AND ii.product.id = :productId " +
            "JOIN b.aisle a " +
            "JOIN a.zone z " +
            "WHERE b.active = true " +
            "AND b.status = 'AVAILABLE' " +
            "AND (:zoneId IS NULL OR z.id = :zoneId) " +
            "AND (b.capacity - b.currentOccupancy) >= :requiredSpace " +
            "ORDER BY " +
            "  CASE WHEN ii.product.id IS NOT NULL THEN 0 ELSE 1 END ASC, " +
            "  (b.capacity - b.currentOccupancy) ASC")
    List<StorageBin> findSmartPutawayBins(
            @Param("productId") String productId,
            @Param("zoneId") String zoneId,
            @Param("requiredSpace") Integer requiredSpace);

    /**
     * Fetches a bin by its code only if it is currently active.
     */
    Optional<StorageBin> findByBinCodeAndActiveTrue(String binCode);
}