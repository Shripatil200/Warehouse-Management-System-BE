package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.entity.BarcodeAudit;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.AuditAction;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.repository.BarcodeAuditRepository;
import com.infotact.warehouse.service.BarcodeAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BarcodeAuditServiceImpl implements BarcodeAuditService {

    private final BarcodeAuditRepository auditRepository;
    private final WarehouseContext warehouseContext;

    // ============================================================
    // SUCCESS LOG
    // ============================================================

    @Override
    public void logSuccess(String userId,
                           String binId,
                           String orderId,
                           AuditAction action,
                           String scannedValue) {

        String warehouseId = warehouseContext.getWarehouseId();

        if (warehouseId == null) {
            throw new IllegalStateException("Warehouse context missing");
        }

        logSuccessAsync(userId, warehouseId, binId, orderId, action, scannedValue);
    }

    @Async
    protected void logSuccessAsync(String userId,
                                   String warehouseId,
                                   String binId,
                                   String orderId,
                                   AuditAction action,
                                   String scannedValue) {

        saveLog(userId, warehouseId, binId, orderId, action,
                AuditStatus.SUCCESS, scannedValue, null);
    }

    // ============================================================
    // FAILURE LOG (🔥 REQUIRED FIX)
    // ============================================================

    @Override
    public void logFailure(String userId,
                           String binId,
                           String orderId,
                           AuditAction action,
                           String scannedValue,
                           String errorReason) {

        String warehouseId = warehouseContext.getWarehouseId();

        if (warehouseId == null) {
            throw new IllegalStateException("Warehouse context missing");
        }

        logFailureAsync(userId, warehouseId, binId, orderId,
                action, scannedValue, errorReason);
    }

    @Async
    protected void logFailureAsync(String userId,
                                   String warehouseId,
                                   String binId,
                                   String orderId,
                                   AuditAction action,
                                   String scannedValue,
                                   String errorReason) {

        saveLog(userId, warehouseId, binId, orderId, action,
                AuditStatus.FAILURE, scannedValue, errorReason);
    }

    // ============================================================
    // CORE LOGIC
    // ============================================================

    private void saveLog(String userId,
                         String warehouseId,
                         String binId,
                         String orderId,
                         AuditAction action,
                         AuditStatus status,
                         String val,
                         String err) {

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        BarcodeAudit log = BarcodeAudit.builder()
                .userId(userId)
                .warehouse(warehouse)
                .binId(binId)
                .orderId(orderId)
                .actionType(action)
                .status(status)
                .scannedValue(val)
                .errorMessage(err)
                .timestamp(LocalDateTime.now())
                .build();

        auditRepository.save(log);
    }
}