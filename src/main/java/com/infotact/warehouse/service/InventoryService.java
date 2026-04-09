package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import jakarta.validation.Valid;

/**
 * Service interface for managing physical stock movements and inventory integrity.
 * <p>
 * This service handles the high-concurrency logic for stock acquisition (Receiving),
 * internal movement (Transferring), and stock reduction (Picking). It ensures
 * that the digital representation of stock perfectly matches the physical reality
 * of the warehouse floor.
 * </p>
 */
public interface InventoryService {

    /**
     * Processes the inbound receipt of goods against a Purchase Order.
     * <p>
     * <b>Workflow & Business Rules:</b>
     * 1. <b>Validation:</b> Verifies the Purchase Order (PO) exists and is in a state
     * capable of receiving (e.g., 'ORDERED' or 'PARTIALLY_RECEIVED').
     * 2. <b>Capacity Check:</b> Cross-references the {@link com.infotact.warehouse.entity.StorageBin}
     * logic to ensure the target location can physically accommodate the quantity.
     * 3. <b>Inventory Update:</b> Upserts the {@link com.infotact.warehouse.entity.InventoryItem}
     * using pessimistic locking to prevent race conditions during simultaneous receipts.
     * 4. <b>PO Status Update:</b> Transitions the Purchase Order status and updates
     * received quantities for audit tracking.
     * </p>
     * * @param request Data containing Product SKU, Quantity, Bin Location, and PO Reference.
     * @throws com.infotact.warehouse.exception.InsufficientStorageException if the target bin is over-capacity.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the SKU or Bin Code is invalid.
     * @throws com.infotact.warehouse.exception.IllegalOperationException if the PO is already fully closed.
     */
    void receiveShipment(@Valid ReceivingRequest request);
}