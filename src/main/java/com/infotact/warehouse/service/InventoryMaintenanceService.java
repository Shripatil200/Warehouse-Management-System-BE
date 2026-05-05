package com.infotact.warehouse.service;

public interface InventoryMaintenanceService {
    /**
     * Identifies and quarantines stock that has exceeded its expiry date.
     */
    void quarantineExpiredStock();
}