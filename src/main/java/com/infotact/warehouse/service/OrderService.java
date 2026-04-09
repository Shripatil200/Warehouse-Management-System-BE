package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import java.util.List;

/**
 * Service interface for managing the Outbound Order lifecycle.
 * <p>
 * This service orchestrates the transition from customer demand to shipment.
 * It handles order validation, stock reservation, and status tracking to
 * ensure high fulfillment accuracy and facility-specific data isolation.
 * </p>
 */
public interface OrderService {

    /**
     * Initializes a new outbound order and validates stock availability.
     * <p>
     * <b>Execution Workflow:</b>
     * 1. <b>Inventory Check:</b> Verifies that the requested quantity for each line item
     * is available across the warehouse's storage bins.
     * 2. <b>Order Persistence:</b> Creates the {@link com.infotact.warehouse.entity.Order}
     * and its associated {@link com.infotact.warehouse.entity.OrderItem} records.
     * 3. <b>Status Initialization:</b> Sets the order to 'PENDING' or 'PLACED' status,
     * signaling to the picking team that work is ready.
     * </p>
     * @param request Data containing customer details and a list of products/quantities.
     * @return The created order details, including a generated Order Reference Number.
     * @throws com.infotact.warehouse.exception.InsufficientStorageException if any item
     * is out of stock (using this exception to signal a "Stock Shortage").
     */
    OrderResponse createOrder(OrderRequest request);

    /**
     * Retrieves the full details of a specific order.
     * @param id The UUID of the order.
     * @return Order details including line items and current status.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the ID is invalid.
     */
    OrderResponse getOrder(String id);

    /**
     * Retrieves orders filtered by status for the current warehouse.
     * <p>
     * <b>Operational Context:</b> Primarily used by the 'Shipping Lead' to manage
     * the daily queue. If 'status' is null, it returns all orders for the facility.
     * </p>
     * @param status Optional filter (e.g., 'PENDING', 'SHIPPED', 'CANCELLED').
     * @return A list of orders belonging strictly to the authenticated user's warehouse.
     */
    List<OrderResponse> getWarehouseOrders(String status);
}