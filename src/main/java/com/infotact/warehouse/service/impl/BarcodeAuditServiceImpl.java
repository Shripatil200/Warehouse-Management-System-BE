package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.entity.BarcodeAudit;
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

    @Async
    @Override
    public void logSuccess(String userId, String warehouseId, String binId, String orderId,
                           AuditAction action, String scannedValue) {
        saveLog(userId, warehouseId, binId, orderId, action, AuditStatus.SUCCESS, scannedValue, null);
    }

    @Async
    @Override
    public void logFailure(String userId, String warehouseId, String binId, String orderId,
                           AuditAction action, String scannedValue, String errorReason) {
        saveLog(userId, warehouseId, binId, orderId, action, AuditStatus.FAILURE, scannedValue, errorReason);
    }

    private void saveLog(String userId, String warehouseId, String binId, String orderId,
                         AuditAction action, AuditStatus status, String val, String err) {
        BarcodeAudit log = BarcodeAudit.builder()
                .userId(userId)
                .warehouseId(warehouseId)
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