package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence entity representing a formal request for stock from a supplier.
 * <p>
 * This entity tracks the 'Inbound' lifecycle. It serves as the source of truth for
 * expected inventory levels, helping managers plan warehouse space before
 * shipments physically arrive at the loading dock.
 * </p>
 */
@Data
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_po_status", columnList = "status"),
        @Index(name = "idx_po_expected", columnList = "expectedDate")
})
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The external vendor providing the products.
     * <p>
     * Linkage: Provides access to supplier contact info and lead times
     * for procurement reporting.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /**
     * The target facility where the stock will be delivered and stored.
     * <p>
     * Anchoring: Essential for multi-warehouse environments to ensure
     * Receiving staff only see orders relevant to their specific building.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * The timestamp when the order was officially placed with the supplier.
     */
    private LocalDateTime orderDate;

    /**
     * The anticipated delivery timestamp.
     * <p>
     * Logic: Used by the Dashboard to calculate 'Pending Inbound' metrics
     * and alert staff of potentially delayed shipments.
     * </p>
     */
    private LocalDateTime expectedDate;

    /**
     * The lifecycle state of the procurement (e.g., PLACED, SHIPPED, RECEIVED).
     * <p>
     * Note: Changing status to 'RECEIVED' typically triggers the actual
     * increase in {@link InventoryItem} quantities.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status;

    /**
     * The itemized list of products and quantities expected from the supplier.
     * <p>
     * Cascading: Managed entirely through the PurchaseOrder lifecycle.
     * </p>
     */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private List<PurchaseOrderItem> items;
}