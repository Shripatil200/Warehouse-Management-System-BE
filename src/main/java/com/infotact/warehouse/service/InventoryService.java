package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.InventoryItem;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Core service responsible for managing physical stock movements and ensuring
 * inventory integrity across a multi-tenant warehouse system.
 *
 * <p>
 * This service represents the transactional backbone of the WMS (Warehouse Management System).
 * It enforces strict rules for:
 * </p>
 *
 * <ul>
 *   <li><b>Cost Layering:</b> Each inventory record represents a distinct financial and physical layer.</li>
 *   <li><b>FEFO Picking:</b> Stock is allocated based on earliest expiry to minimize waste.</li>
 *   <li><b>Concurrency Safety:</b> All critical operations use pessimistic locking.</li>
 *   <li><b>Tenant Isolation:</b> All operations are scoped to the current warehouse via TenantContext.</li>
 *   <li><b>Traceability:</b> Every stock movement generates an immutable transaction record.</li>
 * </ul>
 *
 * <p><b>Inventory Layer Definition:</b></p>
 * A single {@link InventoryItem} is uniquely identified by:
 *
 * <pre>
 * (product + storageBin + batchNumber + purchasePrice + expiryDate)
 * </pre>
 *
 * <p>
 * This ensures accurate cost tracking, FEFO compliance, and audit traceability.
 * </p>
 */
public interface InventoryService {

    /**
     * Processes inbound stock into the warehouse using smart putaway logic.
     *
     * <p><b>Core Behavior:</b></p>
     * <ul>
     *   <li>Selects optimal bin based on capacity, product affinity, and zone.</li>
     *   <li>Validates volumetric and weight constraints.</li>
     *   <li>Ensures strict cost-layering rules.</li>
     * </ul>
     *
     * <p><b>Cost Layering Rules (CRITICAL):</b></p>
     *
     * <ul>
     *   <li>If an existing layer matches:
     *     <pre>
     *     product + bin + batch + purchasePrice + expiryDate
     *     </pre>
     *     → quantity is incremented (merge).</li>
     *
     *   <li>If ANY attribute differs:
     *     <ul>
     *       <li>batch number</li>
     *       <li>expiry date</li>
     *       <li><b>purchase price</b></li>
     *     </ul>
     *     → a NEW inventory layer is created.</li>
     * </ul>
     *
     * <p><b>Duplicate Layer Prevention:</b></p>
     * Prevents multiple rows representing the same logical stock layer.
     * Ensures:
     *
     * <pre>
     * SAME layer → UPDATE
     * DIFFERENT cost → NEW layer
     * </pre>
     *
     * <p><b>Example:</b></p>
     * <ul>
     *   <li>100 units @ $30 → Layer 1</li>
     *   <li>100 units @ $40 → Layer 2 (NOT merged)</li>
     * </ul>
     *
     * @param request inbound shipment details
     */
    void receiveShipment(@Valid ReceivingRequest request);

    /**
     * Performs manual inventory correction.
     *
     * <p><b>Safety Guarantees:</b></p>
     * <ul>
     *   <li>Pessimistic locking ensures no concurrent modification.</li>
     *   <li>Prevents negative stock levels.</li>
     *   <li>Automatically syncs bin weight/volume metrics.</li>
     * </ul>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Cycle counting discrepancies</li>
     *   <li>Damaged goods removal</li>
     *   <li>Stock corrections</li>
     * </ul>
     */
    void adjustStock(@Valid InventoryAdjustmentRequest request);

    /**
     * Reserves stock using FEFO (First Expiry First Out).
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Allocates stock from earliest expiry layers first.</li>
     *   <li>Reserves only AVAILABLE inventory.</li>
     *   <li>Triggers automatic replenishment if pick-face stock is insufficient.</li>
     * </ul>
     *
     * <p><b>Concurrency:</b></p>
     * <ul>
     *   <li>Each inventory layer is locked during reservation.</li>
     *   <li>Prevents double allocation under high concurrency.</li>
     * </ul>
     *
     * @param productId product identifier
     * @param quantity quantity to reserve
     * @return list of reserved inventory layers
     */
    List<InventoryItem> reserveStock(String productId, Integer quantity);

    /**
     * Releases previously reserved stock.
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Order cancellation</li>
     *   <li>Picking failure</li>
     *   <li>Damage discovered during picking</li>
     * </ul>
     *
     * <p><b>Safety:</b></p>
     * <ul>
     *   <li>Prevents reserved quantity from going negative.</li>
     * </ul>
     */
    void releaseReservation(String inventoryItemId, Integer quantity);

    /**
     * Secure picking operation with physical verification.
     *
     * <p><b>Validation Steps:</b></p>
     * <ol>
     *   <li>Validate scanned bin matches reserved bin.</li>
     *   <li>Validate scanned SKU matches product.</li>
     * </ol>
     *
     * <p><b>Purpose:</b></p>
     * Prevents:
     * <ul>
     *   <li>Wrong bin picking</li>
     *   <li>Wrong product picking</li>
     * </ul>
     */
    void commitPickWithVerification(
            String inventoryItemId,
            String scannedBinCode,
            String scannedSku,
            Integer quantity
    );

    /**
     * Finalizes stock deduction from a specific inventory layer.
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Decreases physical quantity</li>
     *   <li>Releases reserved quantity</li>
     *   <li>Updates bin metrics</li>
     * </ul>
     *
     * <p><b>Safety:</b></p>
     * <ul>
     *   <li>Prevents over-picking (reserved < quantity)</li>
     * </ul>
     */
    void commitPick(String inventoryItemId, Integer quantity);

    /**
     * Moves stock between bins (internal transfer).
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Replenishment (Bulk → Pick Face)</li>
     *   <li>Consolidation</li>
     * </ul>
     *
     * <p><b>Critical Rules:</b></p>
     * <ul>
     *   <li>Preserves cost layer (price, batch, expiry MUST remain same)</li>
     *   <li>Locks both source and destination bins</li>
     *   <li>Atomic operation (all-or-nothing)</li>
     * </ul>
     */
    void internalStockTransfer(String sourceItemId, String targetBinId, Integer quantity);

    /**
     * Automated replenishment logic.
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Pulls stock from BULK_STORAGE</li>
     *   <li>Moves it to PICK_FACE</li>
     *   <li>Maintains FEFO order</li>
     * </ul>
     *
     * <p><b>Trigger:</b></p>
     * <ul>
     *   <li>Called automatically when pick-face stock is below threshold</li>
     * </ul>
     */
    void replenishPickingFace(String productId, String targetBinId, Integer desiredQty);
}