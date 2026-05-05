package com.infotact.warehouse.entity.enums;

/**
 * Defines the physical storage format and accessibility of a bin.
 */
public enum BinType {
    PICK_FACE,    // Low-level, easy human access for small quantities
    BULK_STORAGE, // High-level or deep storage for full pallets/cases
    STAGING,      // Temporary area (e.g., at the loading dock)
    RESERVE,      // Overflow storage area
    QUARANTINE    // Area for damaged or expired items under inspection
}