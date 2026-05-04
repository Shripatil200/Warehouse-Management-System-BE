package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import jakarta.validation.Valid;

/**
 * Service interface for managing physical stock movements and inventory integrity.
 * <p>
 * This service handles the high-concurrency logic for stock acquisition (Receiving),
 * internal movement (Transferring), and stock corrections (Adjustment). It ensures
 * that the digital representation of stock perfectly matches the physical reality
 * of the warehouse floor through strict volumetric and financial tracking.
 * </p>
 */
public interface InventoryService {

    /**
     * Processes the inbound receipt of goods into the warehouse.
     * <p>
     * <b>Workflow & Business Rules:</b>
     * 1. <b>Validation:</b> Verifies the Product exists and matches the multi-tenant context.
     * 2. <b>Placement Strategy:</b> If a bin is not specified, triggers the 'Smart Putaway'
     *    engine to find a location based on Category affinity and Zone preferences.
     * 3. <b>Capacity Check:</b> Recalculates Bin occupancy (Volume/Weight) to ensure
     *    the footprint of the shipment fits within the physical constraints of the bin.
     * 4. <b>Cost Layering:</b> Upserts an {@link com.infotact.warehouse.entity.InventoryItem}.
     *    If the purchase price or batch differs from existing stock in that bin, a
     *    new "Cost Layer" (record) is created to maintain valuation accuracy.
     * 5. <b>Audit Trail:</b> Generates an INBOUND transaction ledger entry.
     * </p>
     *
     * @param request Data containing Product ID, Quantity, Optional Bin ID, and Batch metadata.
     * @throws com.infotact.warehouse.exception.InsufficientStorageException if no suitable bin can accommodate the load.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the Product or Bin reference is invalid.
     */
    void receiveShipment(@Valid ReceivingRequest request);

    /**
     * Performs a manual correction of stock levels for a specific inventory layer.
     * <p>
     * <b>Workflow & Business Rules:</b>
     * 1. <b>Concurrency Control:</b> Employs Pessimistic Locking on the target
     *    {@link com.infotact.warehouse.entity.InventoryItem} to prevent race conditions
     *    during simultaneous manual audits.
     * 2. <b>Physical Sync:</b> Automatically adjusts the associated
     *    {@link com.infotact.warehouse.entity.StorageBin} occupancy metrics (Volume/Weight).
     * 3. <b>Integrity Check:</b> Prevents adjustments that would result in negative
     *    physical stock levels.
     * 4. <b>Audit Traceability:</b> Requires a mandatory reason code (e.g., 'DAMAGED',
     *    'LOST', 'CYCLE_COUNT') to be recorded in the transaction ledger.
     * </p>
     *
     * @param request Payload containing the specific Inventory UUID, adjustment delta, and reason.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the specific stock record is not found.
     * @throws com.infotact.warehouse.exception.IllegalOperationException if the adjustment would result in a negative quantity.
     */
    void adjustStock(@Valid InventoryAdjustmentRequest request);


    void reserveStock(String productId, Integer quantity);
    void releaseReservation(String inventoryItemId, Integer quantity);
    void commitPick(String inventoryItemId, Integer quantity);
}