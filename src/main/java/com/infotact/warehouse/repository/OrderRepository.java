package com.infotact.warehouse.repository;

import com.infotact.warehouse.dto.v1.response.ProductSalesDTO;
import com.infotact.warehouse.entity.Order;
import com.infotact.warehouse.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data Access Object for customer Order management.
 * <p>
 * This repository handles the outbound demand. By leveraging the direct
 * denormalized link to the Warehouse entity, it provides high-performance
 * facility-specific lookups and sales analytics.
 * </p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Retrieves all orders belonging to a specific facility.
     */
    List<Order> findAllByWarehouseId(String warehouseId);

    /**
     * RESOLVED: Method to fetch filtered orders for the OrderService.
     * <p>
     * Logic: <code>WHERE warehouse_id = ? AND status = ?</code>
     * </p>
     */
    List<Order> findAllByWarehouseIdAndStatus(String warehouseId, OrderStatus status);

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
    @Query("SELECT COUNT(o) FROM Order o WHERE o.warehouse.id = :warehouseId " +
            "AND o.expectedShipDate < CURRENT_TIMESTAMP " +
            "AND o.status NOT IN (com.infotact.warehouse.entity.enums.OrderStatus.SHIPPED, " +
            "com.infotact.warehouse.entity.enums.OrderStatus.DELIVERED)")
    long countDelayedOrders(@Param("warehouseId") String warehouseId);

    /**
     * ANALYTICS ENGINE: Aggregates units sold per product name.
     * <p>
     * Logic: Groups by product and sums quantity to find top performers.
     * </p>
     */
    @Query("SELECT new com.infotact.warehouse.dto.v1.response.ProductSalesDTO(p.name, SUM(oi.quantity)) " +
            "FROM Order o JOIN o.items oi JOIN oi.product p " +
            "WHERE o.warehouse.id = :warehouseId " +
            "GROUP BY p.name ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesDTO> findTopSellingProducts(@Param("warehouseId") String warehouseId, Pageable pageable);
}