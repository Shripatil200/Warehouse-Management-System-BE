package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Persistence entity representing a single product line in a Purchase Order.
 * <p>
 * Extends {@link BaseEntity} directly — warehouse scoping is inherited through
 * the parent {@link PurchaseOrder} relationship, so a redundant warehouse_id FK
 * is not needed here.
 * </p>
 */
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Total units requested from the supplier. */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Agreed purchase price per unit.
     * Transferred to InventoryItem.purchasePrice on receipt.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;
}
