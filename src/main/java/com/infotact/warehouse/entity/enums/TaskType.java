package com.infotact.warehouse.entity.enums;

/**
 * Describes the physical action an operator must perform for a given task.
 *
 * <p>Each type maps to a specific trigger event:
 * <ul>
 *   <li>{@link #PICKING}    – triggered when a new {@link com.infotact.warehouse.entity.SellingOrder} is created</li>
 *   <li>{@link #PACKING}    – triggered after PICKING is completed on an order</li>
 *   <li>{@link #PUTAWAY}    – triggered when a {@link com.infotact.warehouse.entity.PurchaseOrder} is received at the dock</li>
 *   <li>{@link #RELOCATION} – triggered manually by MANAGER / ADMIN for inventory health</li>
 * </ul>
 */
public enum TaskType {

    /** Retrieve items from bins to fulfil a customer order. */
    PICKING,

    /** Pack the picked items and prepare for shipment. */
    PACKING,

    /** Move inbound shipment goods from the receiving dock to storage bins. */
    PUTAWAY,

    /** Move inventory from one bin to another (consolidation, hazard clearance, etc.). */
    RELOCATION
}
