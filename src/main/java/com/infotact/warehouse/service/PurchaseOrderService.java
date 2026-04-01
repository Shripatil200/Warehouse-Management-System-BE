package com.infotact.warehouse.service;


import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.dto.v1.response.PurchaseOrderResponse;
import com.infotact.warehouse.entity.PurchaseOrder;

import java.util.List;

/**
 * Service interface for managing Purchase Orders (Inbound Stock Expectations).
 * Directives: Ensures absolute data integrity for supplier transactions.
 */
public interface PurchaseOrderService {

    /**
     * Registers a new order with a supplier.
     * Required for Week 2: Transactional Inventory Logic.
     */
    PurchaseOrder createPurchaseOrder(PurchaseOrderRequest request);

    /**
     * Retrieves specific details for a single purchase order.
     */
    PurchaseOrder getPurchaseOrder(String id);

    /**
     * Lists all purchase orders, optionally filtered by status (e.g., PLACED, RECEIVED).
     * Supports macroscopic visibility for Warehouse Managers.
     */
    List<PurchaseOrder> getAllPurchaseOrders(String status);
}