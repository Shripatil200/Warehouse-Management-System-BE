package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.WarehouseScopedEntity;
import com.infotact.warehouse.entity.enums.ConsignmentSettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodic financial settlement for a {@link ConsignmentAgreement}.
 *
 * <p>Created either by the {@code ConsignmentSettlementScheduler} (automatic)
 * or manually triggered by a MANAGER via the API.
 *
 * <p>Each settlement aggregates all unsettled {@link ConsignmentSale} records
 * within its {@code periodFrom} – {@code periodTo} window, computes the total
 * supplier payout, and marks those sales as settled.
 *
 * <p><b>Settlement lifecycle:</b>
 * PENDING → APPROVED (manager signs off) → PAID (payment confirmed)
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "consignment_settlements",
        indexes = {
                @Index(name = "idx_settlement_agreement", columnList = "agreement_id"),
                @Index(name = "idx_settlement_status",    columnList = "status"),
                @Index(name = "idx_settlement_period",    columnList = "periodFrom, periodTo")
        }
)
public class ConsignmentSettlement extends WarehouseScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The agreement this settlement belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private ConsignmentAgreement agreement;

    /**
     * Settlement reference number (e.g., SETL-2026-0042).
     */
    @Column(unique = true, nullable = false)
    private String settlementNumber;

    /**
     * Start of the period covered by this settlement (inclusive).
     */
    @Column(nullable = false)
    private LocalDate periodFrom;

    /**
     * End of the period covered by this settlement (inclusive).
     */
    @Column(nullable = false)
    private LocalDate periodTo;

    /**
     * Total gross revenue from all consigned sales in this period.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalGrossRevenue = BigDecimal.ZERO;

    /**
     * Total amount retained by the warehouse (commission).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalWarehouseShare = BigDecimal.ZERO;

    /**
     * Total amount owed to the supplier (= grossRevenue − warehouseShare).
     * This is the primary payout figure.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalSupplierPayout = BigDecimal.ZERO;

    /**
     * Total units sold across all products in this period.
     */
    @Column(nullable = false)
    private Integer totalUnitsSold = 0;

    /**
     * Current status of this settlement.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsignmentSettlementStatus status = ConsignmentSettlementStatus.PENDING;

    /**
     * Optional notes from the manager (e.g., payment method, reference number).
     */
    @Column(length = 1000)
    private String managerNotes;

    /**
     * Timestamp when the PAID status was confirmed.
     */
    private LocalDateTime paidAt;

    /**
     * All individual sale records included in this settlement.
     * Linked back from {@link ConsignmentSale} com.infotact.warehouse.entity.ConsignmentSale -> settlement.
     */
    @OneToMany(mappedBy = "settlement", fetch = FetchType.LAZY)
    private List<ConsignmentSale> sales;
}
