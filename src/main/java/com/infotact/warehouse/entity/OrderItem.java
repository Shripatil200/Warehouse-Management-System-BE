package com.infotact.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing a single line item within a customer order.
 * <p>
 * This entity specifies the exact product and quantity requested by a customer.
 * During fulfillment, these records are used to calculate total reservations
 * and generate picking lists for warehouse personnel.
 * </p>
 */
@Data
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

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
    private Order order;

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
}