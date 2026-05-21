package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit record created whenever a consigned product is sold.
 *
 * <p><b>When is this created?</b>
 * In {@code OrderService.commitPick()}, after stock is physically deducted,
 * the service checks if the sold product has {@code isConsignment = true}.
 * If so, it creates a {@link ConsignmentSale} record capturing the exact
 * revenue split for that line item.
 *
 * <p><b>Calculation:</b>
 * <pre>
 *   grossRevenue       = sellPrice × quantity
 *   warehouseShare     = grossRevenue × (warehouseCommissionPct / 100)
 *   supplierShare      = grossRevenue − warehouseShare
 * </pre>
 *
 * <p>The pre-computed shares are stored to ensure historical accuracy even
 * if the commission percentage is later renegotiated.
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "consignment_sales",
        indexes = {
                @Index(name = "idx_cs_agreement",   columnList = "agreement_id"),
                @Index(name = "idx_cs_order_item",  columnList = "order_item_id"),
                @Index(name = "idx_cs_settled",     columnList = "settled"),
                @Index(name = "idx_cs_sold_at",     columnList = "soldAt")
        }
)
public class ConsignmentSale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The consignment agreement under which this sale occurred.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private ConsignmentAgreement agreement;

    /**
     * The specific order line item that generated this sale.
     * Provides a direct audit trail back to the customer order.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private SellingOrderItem orderItem;

    /**
     * The product sold (denormalized for fast reporting queries).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Number of units sold in this transaction.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Unit selling price at time of sale.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitSellPrice;

    /**
     * Total revenue: unitSellPrice × quantity.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal grossRevenue;

    /**
     * Commission percentage applied at time of sale
     * (snapshot from the agreement to guard against future renegotiation).
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal warehouseCommissionPctSnapshot;

    /**
     * Warehouse's share of this sale: grossRevenue × (commissionPct / 100).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal warehouseShare;

    /**
     * Supplier's share: grossRevenue − warehouseShare.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal supplierShare;

    /**
     * Timestamp when the order was committed/shipped.
     */
    @Column(nullable = false)
    private LocalDateTime soldAt;

    /**
     * Whether this sale has been included in a settlement cycle.
     * Used by the settlement scheduler to avoid double-counting.
     */
    @Column(nullable = false)
    private boolean settled = false;

    /**
     * The settlement that included this sale (null until settled).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private ConsignmentSettlement settlement;
}
