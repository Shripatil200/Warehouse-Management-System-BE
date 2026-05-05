package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.StorageBin;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.entity.enums.BinType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data Access Object for the physical 'StorageBin' infrastructure.
 * Refactored to eliminate hardcoded paths and optimize transactional integrity.
 */
@Repository
public interface BinRepository extends JpaRepository<StorageBin, String> {

    @Query(value = "SELECT b FROM StorageBin b JOIN FETCH b.aisle WHERE b.aisle.id = :aisleId",
            countQuery = "SELECT count(b) FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Page<StorageBin> findByAisleId(@Param("aisleId") String aisleId, Pageable pageable);

    boolean existsByBinCodeIgnoreCase(String binCode);

    Optional<StorageBin> findByBinCodeAndAisleId(String binCode, String aisleId);

    @Query("SELECT b.binCode FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Set<String> findAllBinCodesByAisleId(@Param("aisleId") String aisleId);

    boolean existsByBinCode(String generatedCode);

    /**
     * THE SMART PUTAWAY ENGINE
     * Uses parameterized Enums to avoid hardcoded package strings.
     *
     * @param productId      The item being stored (for affinity check).
     * @param zoneId         Optional zone restriction.
     * @param reqVolume      Calculated volume of the incoming lot.
     * @param reqWeight      Calculated weight of the incoming lot.
     * @param targetType     Bulk vs Picking (Enum).
     * @param activeStatus   Filter for status (typically BinStatus.AVAILABLE).
     */
    @Query("SELECT DISTINCT b FROM StorageBin b " +
            "LEFT JOIN b.inventoryItems ii WITH ii.product.id = :productId " +
            "JOIN FETCH b.aisle a " +
            "JOIN FETCH a.zone z " +
            "WHERE b.active = true " +
            "AND b.status = :activeStatus " +
            "AND b.binType = :targetType " +
            "AND (:zoneId IS NULL OR z.id = :zoneId) " +
            "AND (b.maxVolume - b.currentVolumeOccupied) >= :reqVolume " +
            "AND (b.maxWeightCapacity - b.currentWeightLoad) >= :reqWeight " +
            "ORDER BY " +
            "  CASE WHEN ii.id IS NOT NULL THEN 0 ELSE 1 END ASC, " + // Product affinity
            "  (b.maxVolume - b.currentVolumeOccupied) ASC")        // Consolidation logic
    List<StorageBin> findSmartPutawayBins(
            @Param("productId") String productId,
            @Param("zoneId") String zoneId,
            @Param("reqVolume") BigDecimal reqVolume,
            @Param("reqWeight") Double reqWeight,
            @Param("targetType") BinType targetType,
            @Param("activeStatus") BinStatus activeStatus);

    /**
     * Finds bin by code with SpEL constant for active state if preferred over params.
     */
    Optional<StorageBin> findByBinCodeAndActiveTrue(String binCode);

    /**
     * CRITICAL: Pessimistic Lock for Capacity Management.
     * Includes a 5-second timeout hint to prevent database deadlock hangs.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT b FROM StorageBin b WHERE b.id = :id")
    Optional<StorageBin> findByIdWithLock(@Param("id") String id);

    /**
     * Fast check for bin availability during scanning operations.
     */
    @Query("SELECT b.status FROM StorageBin b WHERE b.binCode = :binCode AND b.active = true")
    Optional<BinStatus> getBinStatusByCode(@Param("binCode") String binCode);
}