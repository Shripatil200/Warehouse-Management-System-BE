package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.dto.v1.response.PurchaseOrderResponse;
import java.util.List;

/**
 * Service interface for Inbound Procurement and Supply Chain orchestration.
 * <p>
 * This service manages the lifecycle of stock replenishment from external vendors.
 * It acts as the 'Expected Inventory' ledger, allowing the facility to plan
 * labor and storage space ahead of physical shipment arrivals.
 * </p>
 */
public interface PurchaseOrderService {

    /**
     * Registers a new stock replenishment request with a supplier.
     * <p>
     * <b>Execution Workflow:</b>
     * 1. <b>Supplier Verification:</b> Validates that the {@link com.infotact.warehouse.entity.Supplier}
     * is active and authorized.
     * 2. <b>SKU Validation:</b> Ensures all requested items exist in the global
     * {@link com.infotact.warehouse.entity.Product} catalog.
     * 3. <b>Order Generation:</b> Generates a unique PO Reference and persists
     * line items with an initial 'CREATED' or 'PLACED' status.
     * </p>
     * @param request Data containing supplier ID, expected delivery date, and product quantities.
     * @return The persisted PurchaseOrder details.
     */
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);

    /**
     * Retrieves specific details for a single purchase order.
     * <p>
     * <b>Security:</b> This lookup is warehouse-scoped to prevent one facility
     * from viewing the procurement costs or vendor details of another.
     * </p>
     * @param id The internal UUID of the purchase order.
     * @return The detailed order response including line items.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the ID is invalid.
     */
    PurchaseOrderResponse getPurchaseOrder(String id);

    /**
     * Provides a filterable view of all inbound expectations.
     * <p>
     * <b>Operational Usage:</b> Managers use this to identify 'Delayed' shipments
     * or to prepare the receiving dock for 'Shipped' orders.
     * </p>
     * @param status Optional filter (e.g., 'PLACED', 'SHIPPED', 'PARTIALLY_RECEIVED', 'CLOSED').
     * @return A list of matching orders belonging to the authenticated warehouse.
     */
    List<PurchaseOrderResponse> getAllPurchaseOrders(String status);
}