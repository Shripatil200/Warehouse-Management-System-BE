package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.PurchaseOrder;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Inbound Procurement (Purchase Orders).
 * <p>
 * This repository manages the lifecycle of stock replenishment requests.
 * It provides specialized analytics for tracking vendor reliability and
 * facility-specific procurement health.
 * </p>
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    /**
     * Retrieves all purchase records for a specific facility.
     */
    List<PurchaseOrder> findAllByWarehouseId(String warehouseId);

    /**
     * Filters purchase orders by operational status within a warehouse.
     * <p>
     * Usage: Used by the 'Receiving Dock' view to see upcoming or
     * partially received shipments.
     * </p>
     */
    List<PurchaseOrder> findAllByStatusAndWarehouseId(PurchaseOrderStatus status, String warehouseId);

    /**
     * Secure lookup that ensures the order belongs to the requested facility.
     * <p>
     * Isolation: Prevents unauthorized cross-warehouse access to procurement data.
     * </p>
     */
    Optional<PurchaseOrder> findByIdAndWarehouseId(String id, String warehouseId);

    /**
     * Dashboard Metric: Counts orders currently at a specific lifecycle stage.
     */
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.warehouse.id = :warehouseId AND po.status = :status")
    Long countByWarehouseIdAndStatus(@Param("warehouseId") String warehouseId, @Param("status") PurchaseOrderStatus status);

    /**
     * PROCUREMENT HEALTH METRIC: Delayed Shipments.
     * <p>
     * Logic: Identifies orders where the 'expectedDate' has passed but
     * the status is not yet 'RECEIVED'.
     * </p>
     * @param warehouseId The target facility.
     * @param receivedStatus The terminal state to exclude (e.g., RECEIVED).
     * @return Total count of overdue inbound shipments.
     */
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.warehouse.id = :warehouseId " +
            "AND po.expectedDate < CURRENT_TIMESTAMP AND po.status != :receivedStatus")
    Long countDelayedPurchaseOrders(
            @Param("warehouseId") String warehouseId,
            @Param("receivedStatus") PurchaseOrderStatus receivedStatus
    );

    /**
     * Batch lookup for orders matching multiple states.
     * <p>
     * Usage: Used for high-level management reports (e.g., 'In-Progress' orders
     * covering both CREATED and SHIPPED statuses).
     * </p>
     */
    List<PurchaseOrder> findAllByWarehouseIdAndStatusIn(String warehouseId, List<PurchaseOrderStatus> statuses);
}