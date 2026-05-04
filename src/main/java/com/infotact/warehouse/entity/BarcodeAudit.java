package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.AuditAction;
import com.infotact.warehouse.entity.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Historical record of all barcode scanning events.
 * Used for performance tracking, fraud prevention, and inventory accuracy audits.
 */
@Entity
@Table(name = "barcode_audit_logs")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeAudit extends BaseEntity {

    @Column(nullable = false)
    private String warehouseId;

    @Column(nullable = false)
    private String userId; // The Operator who performed the scan

    private String orderId; // Linked order if applicable

    @Column(nullable = false)
    private String binId; // Physical location involved

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction actionType; // e.g., PICK, RECEIVE, ADJUST

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status; // SUCCESS or FAILURE

    private String scannedValue; // The raw data captured by the laser

    private String errorMessage; // Reason for failure (e.g., "Wrong SKU Scanned")

    @Column(nullable = false)
    private LocalDateTime timestamp;
}