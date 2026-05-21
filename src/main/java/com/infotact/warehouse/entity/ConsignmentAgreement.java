package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.TenantAwareEntity;
import com.infotact.warehouse.entity.enums.ConsignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a formal consignment agreement between a Supplier and the Warehouse.
 *
 * <p><b>Business logic:</b>
 * Under a consignment agreement the supplier retains ownership of the goods.
 * The warehouse stores and sells them on the supplier's behalf.
 * When a sale occurs:
 * <ul>
 *   <li>The full selling price is collected by the warehouse.</li>
 *   <li>At settlement time, the warehouse keeps {@code warehouseCommissionPct}
 *       of the revenue and remits the remainder to the supplier.</li>
 * </ul>
 *
 * <p>Each {@link ConsignmentProduct} line within this agreement maps exactly
 * one {@link Product} to consignment terms (MRP, floor price, etc.).
 *
 * <p>Settlement can be triggered manually by a MANAGER or automatically by
 * the scheduled job {@code ConsignmentSettlementScheduler}.
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "consignment_agreements",
        indexes = {
                @Index(name = "idx_consignment_supplier", columnList = "supplier_id"),
                @Index(name = "idx_consignment_status",   columnList = "status"),
                @Index(name = "idx_consignment_warehouse", columnList = "warehouse_id")
        }
)
public class ConsignmentAgreement extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The supplier whose products are being consigned.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /**
     * Percentage of selling revenue retained by the warehouse as commission.
     * Range: 0.00 – 100.00.
     * Example: 15.00 means warehouse keeps 15%, supplier gets 85%.
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal warehouseCommissionPct;

    /**
     * Human-readable reference code (e.g., CONS-2026-0001).
     */
    @Column(unique = true, nullable = false)
    private String agreementCode;

    /**
     * Current lifecycle state of the agreement.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsignmentStatus status = ConsignmentStatus.PENDING_APPROVAL;

    /**
     * Date from which the agreement is effective.
     */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Optional expiry date. Null means open-ended.
     * The scheduler terminates agreements automatically on this date.
     */
    private LocalDate effectiveTo;

    /**
     * Settlement frequency in days (e.g., 30 = monthly settlement).
     * Default: 30.
     */
    @Column(nullable = false)
    private Integer settlementCycleDays = 30;

    /**
     * Free-text notes agreed between supplier and warehouse manager
     * (special handling, return policy, etc.).
     */
    @Column(length = 2000)
    private String notes;

    /**
     * Products included in this consignment agreement.
     */
    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConsignmentProduct> consignmentProducts;

    /**
     * Settlement records produced each cycle.
     */
    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConsignmentSettlement> settlements;
}
