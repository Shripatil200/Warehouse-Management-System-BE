package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Persistence entity representing a single line item within a customer order.
 * <p>
 * Extends {@link BaseEntity} directly — warehouse scoping is inherited through
 * the parent {@link SellingOrder} relationship, so a redundant warehouse_id FK
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
@Table(name = "order_items")
public class SellingOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SellingOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    /** Selling price snapshot at time of order placement. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal sellPriceAtTimeOfOrder;

    /**
     * Cost price snapshot at time of order placement.
     * Zero for consigned products (warehouse bears no COGS).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal costPriceAtTimeOfOrder = BigDecimal.ZERO;

    /**
     * Profit for this line item.
     * Warehouse-owned: (sellPrice - costPrice) × quantity.
     * Consigned: warehouseShare (updated in verifyAndPack after ConsignmentSale is recorded).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal profit = BigDecimal.ZERO;

    /** Whether this line item is a consigned product. */
    @Column(nullable = false)
    private boolean consignment = false;

    /** The ID of the bin where the picker is directed. */
    @Column(name = "suggested_bin_id")
    private String suggestedBinId;

    /** The specific stock layer (InventoryItem) reserved for this order. */
    @Column(name = "inventory_item_id")
    private String inventoryItemId;
}
