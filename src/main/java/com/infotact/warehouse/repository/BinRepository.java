package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.StorageBin;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.entity.enums.ZoneType;
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
 * Repository for StorageBin with warehouse-level data isolation and
 * warehouse-grade putaway intelligence.
 */
@Repository
public interface BinRepository extends JpaRepository<StorageBin, String> {

    // ============================================================
    // PAGINATION (TENANT SAFE)
    // ============================================================

    @Query(value = """
        SELECT b FROM StorageBin b 
        JOIN FETCH b.aisle a 
        WHERE a.id = :aisleId 
        AND b.warehouse.id = :warehouseId
    """,
            countQuery = """
        SELECT count(b) FROM StorageBin b 
        WHERE b.aisle.id = :aisleId 
        AND b.warehouse.id = :warehouseId
    """)
    Page<StorageBin> findByAisleIdAndWarehouseId(
            @Param("aisleId") String aisleId,
            @Param("warehouseId") String warehouseId,
            Pageable pageable
    );

    // ============================================================
    // UNIQUENESS (TENANT SAFE)
    // ============================================================

    boolean existsByBinCodeAndWarehouseId(String binCode, String warehouseId);

    boolean existsByBinCodeIgnoreCaseAndWarehouseId(String binCode, String warehouseId);

    Optional<StorageBin> findByBinCodeAndAisleIdAndWarehouseId(
            String binCode,
            String aisleId,
            String warehouseId
    );

    @Query("""
        SELECT b.binCode FROM StorageBin b 
        WHERE b.aisle.id = :aisleId 
        AND b.warehouse.id = :warehouseId
    """)
    Set<String> findAllBinCodesByAisleIdAndWarehouseId(
            @Param("aisleId") String aisleId,
            @Param("warehouseId") String warehouseId
    );

    // ============================================================
    //  SMART PUTAWAY ENGINE (FINAL)
    // ============================================================

    /**
     * Returns bins ordered by optimal putaway priority:
     *
     * Priority:
     * 0 → Same product bins (consolidation)
     * 1 → Empty bins
     * 2 → Other bins
     *
     * Then sorted by available space (DESC)
     */
    @Query("""
    SELECT DISTINCT b FROM StorageBin b
    JOIN FETCH b.aisle a
    JOIN FETCH a.zone z
    WHERE b.active = true
      AND b.status = :status
      AND b.binType = :binType
      AND z.zoneType = :zoneType
      AND b.warehouse.id = :warehouseId
      AND (:zoneId IS NULL OR z.id = :zoneId)
      AND (b.maxVolume - b.currentVolumeOccupied) >= :reqVolume
      AND (b.maxWeightCapacity - b.currentWeightLoad) >= :reqWeight

    ORDER BY
        CASE
            WHEN EXISTS (
                SELECT 1 FROM InventoryItem ii2
                WHERE ii2.storageBin = b
                  AND ii2.product.id = :productId
            ) THEN 0
            WHEN NOT EXISTS (
                SELECT 1 FROM InventoryItem ii3
                WHERE ii3.storageBin = b
            ) THEN 1
            ELSE 2
        END,
        (b.maxVolume - b.currentVolumeOccupied) DESC
""")
    List<StorageBin> findPutawayCandidates(
            @Param("productId") String productId,
            @Param("zoneId") String zoneId,
            @Param("reqVolume") BigDecimal reqVolume,
            @Param("reqWeight") Double reqWeight,
            @Param("binType") BinType binType,
            @Param("status") BinStatus status,
            @Param("warehouseId") String warehouseId,
            @Param("zoneType") ZoneType zoneType
    );

    // ============================================================
    // LOOKUPS (TENANT SAFE)
    // ============================================================

    Optional<StorageBin> findByBinCodeAndActiveTrueAndWarehouseId(
            String binCode,
            String warehouseId
    );

    @Query("""
        SELECT b.status FROM StorageBin b 
        WHERE b.binCode = :binCode 
        AND b.active = true 
        AND b.warehouse.id = :warehouseId
    """)
    Optional<BinStatus> getBinStatusByCode(
            @Param("binCode") String binCode,
            @Param("warehouseId") String warehouseId
    );

    // ============================================================
    // LOCKING (CRITICAL FOR CONCURRENCY)
    // ============================================================

    Optional<StorageBin> findByIdAndWarehouseId(String id, String warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("""
        SELECT b FROM StorageBin b 
        WHERE b.id = :id 
        AND b.warehouse.id = :warehouseId
    """)
    Optional<StorageBin> findByIdWithLock(
            @Param("id") String id,
            @Param("warehouseId") String warehouseId
    );
}