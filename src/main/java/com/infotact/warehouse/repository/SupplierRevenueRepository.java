package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ConsignmentSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only aggregation repository for Supplier Revenue reporting.
 * Piggy-backs on ConsignmentSale as the backing entity but queries across
 * PurchaseOrderItem and BinRentalPayment via JPQL joins.
 */
@Repository
public interface SupplierRevenueRepository extends JpaRepository<ConsignmentSale, String> {

    /**
     * Aggregates consignment commission earned per supplier within a warehouse
     * for a given date range.
     *
     * Returns Object[] rows: [supplierId (String), commissionTotal (BigDecimal)]
     */
    @Query("""
           SELECT cs.agreement.supplier.id,
                  COALESCE(SUM(cs.warehouseShare), 0)
           FROM ConsignmentSale cs
           WHERE cs.agreement.warehouse.id = :warehouseId
             AND cs.soldAt >= :from
             AND cs.soldAt <= :to
           GROUP BY cs.agreement.supplier.id
           """)
    List<Object[]> sumCommissionPerSupplier(
            @Param("warehouseId") String warehouseId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Commission for a single supplier.
     */
    @Query("""
           SELECT COALESCE(SUM(cs.warehouseShare), 0)
           FROM ConsignmentSale cs
           WHERE cs.agreement.warehouse.id = :warehouseId
             AND cs.agreement.supplier.id = :supplierId
             AND cs.soldAt >= :from
             AND cs.soldAt <= :to
           """)
    BigDecimal sumCommissionForSupplier(
            @Param("warehouseId") String warehouseId,
            @Param("supplierId") String supplierId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Total commission revenue earned by warehouse across all suppliers in a date range.
     * Used for dashboard this-month block.
     */
    @Query("""
           SELECT COALESCE(SUM(cs.warehouseShare), 0)
           FROM ConsignmentSale cs
           WHERE cs.agreement.warehouse.id = :warehouseId
             AND cs.soldAt >= :from
             AND cs.soldAt <= :to
           """)
    BigDecimal sumTotalCommission(
            @Param("warehouseId") String warehouseId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Aggregates total purchase spend per supplier (sum of unitCost × quantity on
     * RECEIVED purchase orders) within a warehouse for a date range.
     *
     * Returns Object[] rows: [supplierId (String), totalSpend (BigDecimal)]
     */
    @Query("""
           SELECT poi.purchaseOrder.supplier.id,
                  COALESCE(SUM(poi.unitCost * poi.quantity), 0)
           FROM PurchaseOrderItem poi
           WHERE poi.purchaseOrder.warehouse.id = :warehouseId
             AND poi.purchaseOrder.status = 'RECEIVED'
             AND poi.purchaseOrder.orderDate >= :from
             AND poi.purchaseOrder.orderDate <= :to
           GROUP BY poi.purchaseOrder.supplier.id
           """)
    List<Object[]> sumSpendPerSupplier(
            @Param("warehouseId") String warehouseId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Purchase spend for a single supplier.
     */
    @Query("""
           SELECT COALESCE(SUM(poi.unitCost * poi.quantity), 0)
           FROM PurchaseOrderItem poi
           WHERE poi.purchaseOrder.warehouse.id = :warehouseId
             AND poi.purchaseOrder.supplier.id = :supplierId
             AND poi.purchaseOrder.status = 'RECEIVED'
             AND poi.purchaseOrder.orderDate >= :from
             AND poi.purchaseOrder.orderDate <= :to
           """)
    BigDecimal sumSpendForSupplier(
            @Param("warehouseId") String warehouseId,
            @Param("supplierId") String supplierId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
