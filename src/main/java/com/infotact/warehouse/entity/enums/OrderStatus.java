package com.infotact.warehouse.entity.enums;

public enum OrderStatus {
    PENDING,   // Stock Reserved
    PICKING,   // Picking document generated
    PICKED,    // Verified and picked, waiting to be packed
    PACKED,    // Packed, waiting to move to shipping zone
    SHIPPED,   // Physically left the building
    CANCELLED,  // releaseReservation() executed
    DELIVERED
}