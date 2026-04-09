package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Persistence entity acting as an immutable ledger for all stock movements.
 * <p>
 * Every physical change to inventory (receiving, picking, adjusting, or moving)
 * must generate an InventoryTransaction record. This provides a complete
 * audit trail for security and inventory reconciliation.
 * </p>
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_transactions")
@EntityListeners(AuditingEntityListener.class)
public class InventoryTransaction {

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
     * Example: A Purchase Order ID for INBOUND or a Sales Order ID for OUTBOUND.
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
}