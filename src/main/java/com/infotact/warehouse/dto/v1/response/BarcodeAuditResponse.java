package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.AuditAction;
import com.infotact.warehouse.entity.enums.AuditStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BarcodeAuditResponse {
    private String auditId;
    private LocalDateTime timestamp;
    private String userId;
    private String binCode; // Resolved from binId
    private AuditAction actionType;
    private AuditStatus status; // SUCCESS or FAILURE
    private String scannedValue;
    private String errorMessage;
}