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
 * Represents a formal consignment agreement between a {@link Supplier} and a Warehouse.
 * <p>
 * Under a consignment agreement the supplier retains ownership of the goods.
 * The warehouse stores and sells them on the supplier's behalf, keeping a commission
 * percentage of each sale and remitting the remainder at settlement time.
 * </p>
 * <p>
 * The {@code supplier} field references the dedicated {@link Supplier} entity.
 * </p>
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
                @Index(name = "idx_consignment_supplier",  columnList = "supplier_id"),
                @Index(name = "idx_consignment_status",    columnList = "status"),
                @Index(name = "idx_consignment_warehouse", columnList = "warehouse_id")
        }
)
public class ConsignmentAgreement extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The supplier whose products are being consigned.
     * References the dedicated {@link Supplier} entity — not User.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** Percentage of selling revenue retained by the warehouse as commission (0.00–100.00). */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal warehouseCommissionPct;

    /** Human-readable reference code (e.g., CONS-2026-0001). */
    @Column(unique = true, nullable = false)
    private String agreementCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsignmentStatus status = ConsignmentStatus.PENDING_APPROVAL;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /** Optional expiry date. Null means open-ended. */
    private LocalDate effectiveTo;

    /** Settlement frequency in days. Default: 30. */
    @Column(nullable = false)
    private Integer settlementCycleDays = 30;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConsignmentProduct> consignmentProducts;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConsignmentSettlement> settlements;
}
