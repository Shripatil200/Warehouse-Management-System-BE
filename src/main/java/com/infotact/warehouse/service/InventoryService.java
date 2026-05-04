package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.InventoryItem;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Service interface for managing physical stock movements and inventory integrity.
 * <p>
 * This service handles high-concurrency logic for stock acquisition (Receiving),
 * internal movement, and secure fulfillment. It ensures the digital "Digital Twin"
 * of the stock matches physical reality through strict FEFO logic and
 * mandatory scan-validation protocols.
 * </p>
 */
public interface InventoryService {

    /**
     * Processes the inbound receipt of goods into the warehouse.
     * <p>
     * <b>Workflow:</b>
     * 1. <b>Smart Putaway:</b> Finds an optimal location based on product category.
     * 2. <b>Volumetric Check:</b> Ensures weight and volume fit the physical bin[cite: 1].
     * 3. <b>Cost Layering:</b> Creates a new record if Batch or Expiry differs from existing stock[cite: 1].
     * </p>
     *
     * @param request Data containing Product ID, Quantity, and Batch metadata.
     */
    void receiveShipment(@Valid ReceivingRequest request);

    /**
     * Performs a manual correction of stock levels for a specific inventory layer.
     * <p>
     * <b>Process:</b>
     * Uses Pessimistic Locking to prevent race conditions during manual audits[cite: 1].
     * Automatically adjusts the associated bin occupancy metrics[cite: 1].
     * </p>
     *
     * @param request Payload containing Inventory UUID, adjustment delta, and reason.
     */
    void adjustStock(@Valid InventoryAdjustmentRequest request);

    /**
     * Reserves stock using FEFO (First-Expiry-First-Out) logic.
     * <p>
     * <b>Industry Purpose:</b> Soft-locks the oldest expiring items first to minimize waste[cite: 1].
     * Returns the specific inventory layers and their Bin IDs to guide pickers[cite: 1].
     * </p>
     *
     * @param productId The UUID of the product to reserve.
     * @param quantity The total quantity required for the order.
     * @return A list of specific {@link InventoryItem} layers reserved[cite: 1].
     */
    List<InventoryItem> reserveStock(String productId, Integer quantity);

    /**
     * Releases a soft-lock reservation, returning items to the available pool.
     * <p>
     * Typically used when an order is cancelled or a picker discovers damaged goods[cite: 1].
     * </p>
     *
     * @param inventoryItemId The digital record ID to release.
     * @param quantity The quantity to return to 'Available' status.
     */
    void releaseReservation(String inventoryItemId, Integer quantity);

    /**
     * <b>SECURE FULFILLMENT:</b> Commits a pick only after physical verification.
     * <p>
     * <b>Verification Protocol:</b>
     * 1. Validates that the physical bin scan matches the reserved location[cite: 1].
     * 2. Validates that the physical product scan matches the required SKU.
     * </p>
     *
     * @param inventoryItemId The digital record ID.
     * @param scannedBinCode The raw barcode text from the physical rack scan.
     * @param scannedSku The raw barcode text from the product sticker scan.
     * @param quantity The quantity physically picked.
     */
    void commitPickWithVerification(String inventoryItemId, String scannedBinCode, String scannedSku, Integer quantity);

    /**
     * Finalizes physical deduction of stock from a specific inventory layer.
     * <p>
     * Decrements physical stock, releases the soft-lock, and updates bin metrics[cite: 1].
     * </p>
     *
     * @param inventoryItemId The ID of the inventory record being picked.
     * @param quantity The amount being removed from the warehouse.
     */
    void commitPick(String inventoryItemId, Integer quantity);
}