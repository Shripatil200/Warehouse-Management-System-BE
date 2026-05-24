package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import com.infotact.warehouse.entity.SellingOrder;
import com.infotact.warehouse.entity.SellingOrderItem;
import com.infotact.warehouse.entity.enums.OrderStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for managing the Outbound SellingOrder lifecycle.
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
     * 2. <b>SellingOrder Persistence:</b> Creates the {@link SellingOrder}
     * and its associated {@link SellingOrderItem} records.
     * 3. <b>Status Initialization:</b> Sets the order to 'PENDING' or 'PLACED' status,
     * signaling to the picking team that work is ready.
     * </p>
     * @param request Data containing customer details and a list of products/quantities.
     * @return The created order details, including a generated SellingOrder Reference Number.
     * @throws com.infotact.warehouse.exception.InsufficientStorageException if any item
     * is out of stock (using this exception to signal a "Stock Shortage").
     */
    OrderResponse createOrder(OrderRequest request);

    /**
     * Retrieves the full details of a specific order.
     * @param id The UUID of the order.
     * @return SellingOrder details including line items and current status.
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
    Page<OrderResponse> getWarehouseOrders(String status, Pageable pageable);


    /**
     * Verifies a single physical scan and commits the pick for that inventory item.
     * Called once per item scan from the operator's handheld device.
     * When all items in the order are picked, the order status advances to PACKED.
     *
     * @param orderId         UUID of the order being fulfilled.
     * @param inventoryItemId The specific stock layer reserved for this pick.
     * @param scannedSku      Raw barcode from the product label (must match the reserved layer).
     * @param scannedBinCode  Raw barcode from the bin label (must match the reserved bin).
     * @param quantity        Units physically removed from the bin.
     */
    void verifyAndPack(String orderId, String inventoryItemId,
                       String scannedSku, String scannedBinCode, int quantity);

    /**
     * Transitions an order through its operational lifecycle and triggers associated inventory actions.
     * <p>
     * <b>State Machine & Inventory Integration:</b>
     * <ul>
     *     <li><b>PENDING &rarr; PICKING:</b> Validates that the order is ready for warehouse staff to begin work.</li>
     *     <li><b>PICKING &rarr; PACKED:</b> Triggers {@code inventoryService.commitPick()}. This physically deducts
     *     quantities from {@code InventoryItem} records and synchronizes {@code StorageBin} occupancy.</li>
     *     <li><b>PACKED &rarr; SHIPPED:</b> Finalizes the fulfillment process for outbound logistics.</li>
     *     <li><b>ANY &rarr; CANCELLED:</b> Triggers {@code inventoryService.releaseReservation()}. This reverses
     *     the "soft-lock" on stock, returning it to the available pool for other orders.[cite: 1]</li>
     * </ul>
     * </p>
     * <p>
     * <b>Security & Validation:</b>
     * 1. <b>Facility Isolation:</b> Ensures the order belongs to the authenticated manager's warehouse.[cite: 1]
     * 2. <b>Sequential Integrity:</b> Prevents skipping states (e.g., cannot move from PENDING to SHIPPED).[cite: 1]
     * 3. <b>Atomic Transactions:</b> If an inventory deduction fails during the PACKED transition, the
     * status change is rolled back to maintain data consistency.[cite: 1]
     * </p>
     *
     * @param orderId    The UUID of the order to transition.[cite: 1]
     * @param nextStatus The target {@link OrderStatus} for the fulfillment stage.[cite: 1]
     * @return The updated order details with the new status and timestamp.[cite: 1]
     * @throws com.infotact.warehouse.exception.IllegalOperationException if the status transition is logically invalid.[cite: 1]
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the order does not exist or is outside the user's scope.[cite: 1]
     */
    OrderResponse updateOrderStatus(String orderId, OrderStatus nextStatus);
}