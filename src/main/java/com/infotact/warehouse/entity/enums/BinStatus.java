package com.infotact.warehouse.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the physical and operational state of a Storage Bin.
 * <p>
 * These statuses dictate the behavior of the Smart Putaway and Picking engines.
 * </p>
 */
@Schema(description = "Operational status of a storage bin")
public enum BinStatus {
    /** Bin is empty and ready for placement. */
    EMPTY,

    /** Bin contains some stock and has remaining capacity for more. */
    AVAILABLE,

    /** Bin has reached its volumetric or weight limit (typically >95%). */
    FULL,

    /** Bin is physically unusable (e.g., damaged rack, spill, or cleaning). */
    BLOCKED,

    /** Bin is committed to a specific incoming shipment to ensure space is held. */
    RESERVED,

    /** Bin is logically removed from the warehouse layout. */
    INACTIVE
}