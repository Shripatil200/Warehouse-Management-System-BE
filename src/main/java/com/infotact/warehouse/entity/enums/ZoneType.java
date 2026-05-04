package com.infotact.warehouse.entity.enums;

public enum ZoneType {
    PICKING,    // Fast access for small orders
    BULK,       // Deep storage for full pallets
    RECEIVING,  // Temporary inbound area
    SHIPPING,   // Staging for outbound
    RETURN      // Quality check area
}
