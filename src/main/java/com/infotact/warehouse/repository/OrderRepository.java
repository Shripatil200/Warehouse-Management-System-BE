package com.infotact.warehouse.repository;

import com.infotact.warehouse.dto.v1.response.ProductSalesDTO;
import com.infotact.warehouse.entity.SellingOrder;
import com.infotact.warehouse.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Data Access Object for customer SellingOrder management.
 * <p>
 * This repository handles the outbound demand. By leveraging the direct
 * denormalized link to the Warehouse entity, it provides high-performance
 * facility-specific lookups and sales analytics.
 * </p>
 */
@Repository
public interface OrderRepository extends JpaRepository<SellingOrder, String> {

    /**
     * Retrieves all orders belonging to a specific facility.
     */
    List<SellingOrder> findAllByWarehouseId(String warehouseId);

    /**
     * Paginated retrieval of all orders for a facility.
     */
    org.springframework.data.domain.Page<SellingOrder> findAllByWarehouseId(String warehouseId, org.springframework.data.domain.Pageable pageable);

    /**
     * Paginated retrieval of orders filtered by status.
     */
    org.springframework.data.domain.Page<SellingOrder> findAllByWarehouseIdAndStatus(String warehouseId, OrderStatus status, org.springframework.data.domain.Pageable pageable);

    /**
     * Aggregates count of orders by status for dashboard metrics.
     */
    long countByWarehouseIdAndStatus(String warehouseId, OrderStatus status);

    /**
     * LOGIC RESOLVER: Identifies truly delayed orders for Dashboard alerts.
     * <p>
     * Counts orders that have passed their expected shipment timestamp
     * but are not yet finalized (SHIPPED or DELIVERED).
     * </p>
     */
    @Query("SELECT COUNT(o) FROM SellingOrder o WHERE o.warehouse.id = :warehouseId " +
            "AND o.expectedShipDate < CURRENT_TIMESTAMP " +
            "AND o.status NOT IN :excludedStatuses")
    long countDelayedOrders(
            @Param("warehouseId") String warehouseId,
            @Param("excludedStatuses") Collection<OrderStatus> excludedStatuses
    );

    /**
     * ANALYTICS ENGINE: Aggregates units sold per product name.
     * <p>
     * Logic: Groups by product and sums quantity to find top performers.
     * </p>
     */
    @Query("SELECT new com.infotact.warehouse.dto.v1.response.ProductSalesDTO(p.name, SUM(oi.quantity)) " +
            "FROM SellingOrder o JOIN o.items oi JOIN oi.product p " +
            "WHERE o.warehouse.id = :warehouseId " +
            "GROUP BY p.name ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesDTO> findTopSellingProducts(@Param("warehouseId") String warehouseId, Pageable pageable);

    @Query("SELECT new com.infotact.warehouse.dto.v1.response.ProductSalesDTO(p.name, SUM(oi.quantity)) " +
            "FROM SellingOrder o JOIN o.items oi JOIN oi.product p " +
            "WHERE o.warehouse.id = :warehouseId AND o.createdAt >= :startDate " +
            "GROUP BY p.name ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesDTO> findTopSellingProductsMonthly(
            @Param("warehouseId") String warehouseId,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable);
}