package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.entity.PurchaseOrder;
import java.util.List;

/**
 * Service interface for managing Purchase Orders (Inbound Stock Expectations).
 * Directives: Ensures absolute data integrity for supplier transactions.
 */
public interface PurchaseOrderService {

    /**
     * Registers a new order with a supplier.
     * Maps incoming DTO to PurchaseOrder and child PurchaseOrderItems.
     * * @param request Data containing supplier ID and list of product SKUs/quantities.
     * @return The persisted PurchaseOrder entity.
     */
    PurchaseOrder createPurchaseOrder(PurchaseOrderRequest request);

    /**
     * Retrieves specific details for a single purchase order.
     * * @param id The internal UUID of the purchase order.
     * @return The PurchaseOrder entity.
     * @throws com.infotact.warehouse.exception.EntityNotFoundException if the ID is invalid.
     */
    PurchaseOrder getPurchaseOrder(String id);

    /**
     * Lists all purchase orders, optionally filtered by status (e.g., PLACED, RECEIVED).
     * Supports macroscopic visibility for Warehouse Managers.
     * @param status Optional string filter for order status.
     * @return List of matching PurchaseOrder entities.
     */
    List<PurchaseOrder> getAllPurchaseOrders(String status);
}