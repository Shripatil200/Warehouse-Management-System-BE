package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.WarehouseScopedEntity;
import com.infotact.warehouse.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence entity acting as an immutable ledger for all stock movements.
 * <p>
 * Every physical change to inventory (receiving, picking, adjusting)
 * generates a record for security and inventory reconciliation.
 * </p>
 * <p>
 * <b>Update:</b> Added unitPrice to record the financial value of the movement.
 * For INBOUND, this is the purchase price; for OUTBOUND, it is the selling price.
 * </p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_transactions")
@EntityListeners(AuditingEntityListener.class)
public class InventoryTransaction extends WarehouseScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The specific inventory record affected by this movement.
     * <p>
     * Linkage: Connects the transaction to a specific Product-Bin-Batch combination.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    /**
     * The nature of the stock movement.
     * <p>
     * Values include INBOUND (Suppliers), OUTBOUND (Customers),
     * TRANSFER (Internal moves), and ADJUSTMENT (Corrections).
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /**
     * The net change in quantity.
     * <p>
     * Use positive values for additions and negative values for subtractions.
     * Example: Receiving 10 units = +10; Picking 5 units = -5.
     * </p>
     */
    @Column(nullable = false)
    private Long quantityChange;

    /**
     * External document reference for traceability.
     * <p>
     * Example: A Purchase SellingOrder ID for INBOUND or a Sales SellingOrder ID for OUTBOUND.
     * This allows users to "drill down" into the source of the movement.
     * </p>
     */
    private String referenceId;

    /**
     * Brief explanation for manual changes or specific events.
     * <p>
     * Example: "DAMAGED_IN_TRANSIT" or "STOCK_TAKE_CORRECTION".
     * </p>
     */
    private String reasonCode;

    /**
     * Automated timestamp of when the transaction occurred.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime transactionDate;

    /**
     * The identifier of the staff member who performed the action.
     * <p>
     * Captured automatically via Spring Security context integration.
     * </p>
     */
    @CreatedBy
    private String performedBy;

    /**
     * The financial value assigned to this specific transaction.
     * <p>
     * Logic: Allows auditors to see the total value of "Damaged" goods or
     * the total value of stock received in a given month.
     * </p>
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal unitPrice;

}