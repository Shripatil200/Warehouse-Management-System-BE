package com.infotact.warehouse.entity.enums;

public enum OrderStatus {
    PENDING,   // Stock Reserved
    PICKING,   // Picking document generated
    PACKED,    // commitPick() executed
    SHIPPED,   // Physically left the building
    CANCELLED,  // releaseReservation() executed
    DELIVERED
}