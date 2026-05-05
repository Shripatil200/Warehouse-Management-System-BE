package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.AuditAction;
import com.infotact.warehouse.entity.enums.AuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Historical record of all barcode scanning events.
 * Used for performance tracking, fraud prevention, and inventory accuracy audits.
 */
@Entity
@Table(name = "barcode_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // Important for entities extending a base class
public class BarcodeAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Internal unique identifier (UUID)")
    private String id; // Fixed: Added the missing primary key identifier

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

    /**
     * Lifecycle hook to ensure the timestamp is captured at the moment of the event.
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}