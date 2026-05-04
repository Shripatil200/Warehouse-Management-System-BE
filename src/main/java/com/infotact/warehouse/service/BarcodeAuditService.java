package com.infotact.warehouse.service;

import com.infotact.warehouse.entity.enums.AuditAction;

/**
 * Service for recording physical interactions between staff and inventory.
 * Bridges the gap between digital state changes and physical proof.
 */
public interface BarcodeAuditService {

    /**
     * Records a successful barcode verification event.
     */
    void logSuccess(String userId, String warehouseId, String binId, String orderId,
                    AuditAction action, String scannedValue);

    /**
     * Records a failed verification attempt with the specific reason for failure.
     */
    void logFailure(String userId, String warehouseId, String binId, String orderId,
                    AuditAction action, String scannedValue, String errorReason);
}