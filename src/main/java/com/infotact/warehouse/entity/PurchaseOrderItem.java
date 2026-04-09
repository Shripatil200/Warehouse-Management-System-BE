package com.infotact.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing a specific product line item in a Purchase Order.
 * <p>
 * This entity defines the quantity of a product expected from a supplier.
 * During the receiving process, these records are used as a checklist to
 * verify physical shipment accuracy before updating inventory levels.
 * </p>
 */
@Data
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The parent Purchase Order this line item is associated with.
     * <p>
     * Logic: Standard many-to-one relationship. Inbound stock logic
     * aggregates these items to determine total pending inventory.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * The product being procured.
     * <p>
     * Linkage: Connects the inbound shipment to the master catalog,
     * ensuring the correct SKU is updated upon receipt.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The total count of units requested from the supplier.
     * <p>
     * Usage: This value is compared against the 'received quantity'
     * during the inbound verification process to identify short-shipments.
     * </p>
     */
    @Column(nullable = false)
    private Integer quantity;
}