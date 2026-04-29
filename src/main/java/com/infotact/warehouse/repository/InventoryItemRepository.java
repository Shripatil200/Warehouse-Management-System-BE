package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.InventoryItem;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Calculates the total financial valuation.
     * We MUST use the full path because the DB needs to know the Joins.
     */
    @Query("SELECT SUM(i.quantity * i.purchasePrice) FROM InventoryItem i " +
            "WHERE i.storageBin.aisle.zone.warehouse.id = :warehouseId")
    BigDecimal calculateTotalValueByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * Optimized fetch to avoid N+1 when calculating occupancy for the frontend.
     */
    @Query("SELECT i FROM InventoryItem i " +
            "JOIN FETCH i.storageBin b " +
            "JOIN FETCH b.aisle a " +
            "JOIN FETCH a.zone z " +
            "WHERE z.warehouse.id = :warehouseId")
    List<InventoryItem> findByWarehouseWithHierarchy(@Param("warehouseId") String warehouseId);
}