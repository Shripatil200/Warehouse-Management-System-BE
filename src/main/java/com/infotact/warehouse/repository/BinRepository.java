package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.StorageBin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface BinRepository extends JpaRepository<StorageBin, String> {

    /**
     * OPERATIONAL VIEW: Used for lazy loading bins by their parent aisle.
     * Supports the 200ms response time KPI through pagination.
     */
    Page<StorageBin> findByAisleId(String aisleId, Pageable pageable);

    /**
     * For unique bin code validation during creation.
     */
    boolean existsByBinCodeIgnoreCase(String binCode);

    /**
     * Finds a specific bin and ensures it belongs to the correct warehouse/aisle.
     * Vital for the "Receiving & Putaway Engine" in Week 2.
     */
    Optional<StorageBin> findByBinCodeAndAisleId(String binCode, String aisleId);

    // Optimized: Fetches only the Strings, not the whole object
    @Query("SELECT b.binCode FROM StorageBin b WHERE b.aisle.id = :aisleId")
    Set<String> findAllBinCodesByAisleId(@Param("aisleId") String aisleId);

    boolean existsByBinCode(String generatedCode);
}