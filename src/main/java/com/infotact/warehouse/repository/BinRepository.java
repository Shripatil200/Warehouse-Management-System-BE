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
 * Tenant-safe repository for StorageBin.
 *
 * <p>
 * All queries are scoped by warehouseId to enforce strict multi-tenancy.
 * </p>
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
    // SMART PUTAWAY (TENANT SAFE)
    // ============================================================

    @Query("""
        SELECT DISTINCT b FROM StorageBin b 
        LEFT JOIN b.inventoryItems ii WITH ii.product.id = :productId 
        JOIN FETCH b.aisle a 
        JOIN FETCH a.zone z 
        WHERE b.active = true 
        AND b.status = :activeStatus 
        AND b.binType = :targetType 
        AND b.warehouse.id = :warehouseId
        AND (:zoneId IS NULL OR z.id = :zoneId) 
        AND (b.maxVolume - b.currentVolumeOccupied) >= :reqVolume 
        AND (b.maxWeightCapacity - b.currentWeightLoad) >= :reqWeight 
        ORDER BY 
            CASE WHEN ii.id IS NOT NULL THEN 0 ELSE 1 END ASC,
            (b.maxVolume - b.currentVolumeOccupied) ASC
    """)
    List<StorageBin> findSmartPutawayBins(
            @Param("productId") String productId,
            @Param("zoneId") String zoneId,
            @Param("reqVolume") BigDecimal reqVolume,
            @Param("reqWeight") Double reqWeight,
            @Param("targetType") BinType targetType,
            @Param("activeStatus") BinStatus activeStatus,
            @Param("warehouseId") String warehouseId
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
    // LOCKING (SAFE)
    // ============================================================

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