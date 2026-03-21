package com.infotact.warehouse.entity;

public enum BinStatus {
    AVAILABLE,
    FULL,
    BLOCKED,    // Under maintenance or spill
    RESERVED,   // Scheduled for an incoming PO
    INACTIVE    // Retired from use
}
