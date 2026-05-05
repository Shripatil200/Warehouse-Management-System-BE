package com.infotact.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Persistence entity representing a single line item within a customer order.
 * <p>
 * Specifies the exact product and quantity requested. Used to calculate
 * reservations and generate picking lists for warehouse personnel.
 * </p>
 * <p>
 * <b>Update:</b> Added sellPriceAtTimeOfOrder to act as a financial snapshot.
 * If the master Product price changes, this record preserves what the customer paid.
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
public class SellingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The parent order this line item belongs to.
     * <p>
     * Logic: Standard many-to-one relationship. Deleting an item here
     * should ideally only happen if the order is in a 'PENDING' state.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SellingOrder order;

    /**
     * The specific product referenced in this line item.
     * <p>
     * Linkage: Provides access to SKU and weight details necessary
     * for packaging and shipping calculations.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The number of units requested for this specific product.
     * <p>
     * Validation: Must be a positive integer, typically enforced at
     * the DTO level before persisting.
     * </p>
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * The selling price captured at the moment the order was placed.
     * <p>
     * Logic: Prevents "back-dating" of revenue. Even if Product.sellingPrice
     * is updated to 20rs, an order placed at 15rs will always show 15rs.
     * </p>
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal sellPriceAtTimeOfOrder;

    /**
     * The ID of the bin where the picker is physically directed.
     */
    @Column(name = "suggested_bin_id")
    private String suggestedBinId;

    /**
     * The specific stock layer (InventoryItem) reserved for this order.
     */
    @Column(name = "inventory_item_id")
    private String inventoryItemId;

}