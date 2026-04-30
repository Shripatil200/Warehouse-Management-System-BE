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
 * Updated to support Volumetric and Weight-based "Smart Putaway" logic.
 * </p>
 */
@Repository
public interface BinRepository extends JpaRepository<StorageBin, String> {

    Page<StorageBin> findByAisleId(String aisleId, Pageable pageable);

    boolean existsByBinCodeIgnoreCase(String binCode);

    Optional<StorageBin> findByBinCodeAndAisleId(String binCode, String aisleId);

    @Query("SELECT b.binCode FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Set<String> findAllBinCodesByAisleId(@Param("aisleId") String aisleId);

    boolean existsByBinCode(String generatedCode);

    /**
     * THE SMART PUTAWAY ENGINE (Volumetric & Weight Aware)
     * <p>
     * Implements an intelligent suggestion algorithm:
     * 1. <b>Affinity:</b> Bins with the same product first.
     * 2. <b>Physical Fit:</b> Checks BOTH (maxVolume - currentVolume) AND (maxWeight - currentWeight).
     * 3. <b>Consolidation:</b> Prioritizes bins that are already partially full to save empty bins.
     * </p>
     * @param productId ID of item to receive.
     * @param zoneId Optional zone filter.
     * @param reqVolume Total volume of the shipment (qty * L * W * H).
     * @param reqWeight Total weight of the shipment (qty * weight).
     */
    @Query("SELECT b FROM StorageBin b " +
            "LEFT JOIN b.inventoryItems ii ON ii.product.id = :productId " +
            "JOIN b.aisle a " +
            "JOIN a.zone z " +
            "WHERE b.active = true " +
            "AND b.status = 'AVAILABLE' " +
            "AND (:zoneId IS NULL OR z.id = :zoneId) " +
            "AND (b.maxVolume - b.currentVolumeOccupied) >= :reqVolume " +
            "AND (b.maxWeightCapacity - b.currentWeightLoad) >= :reqWeight " +
            "ORDER BY " +
            "  CASE WHEN ii.product.id IS NOT NULL THEN 0 ELSE 1 END ASC, " +
            "  (b.maxVolume - b.currentVolumeOccupied) ASC")
    List<StorageBin> findSmartPutawayBins(
            @Param("productId") String productId,
            @Param("zoneId") String zoneId,
            @Param("reqVolume") Double reqVolume,
            @Param("reqWeight") Double reqWeight);

    Optional<StorageBin> findByBinCodeAndActiveTrue(String binCode);
}