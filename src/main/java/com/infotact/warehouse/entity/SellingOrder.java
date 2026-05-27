package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.WarehouseScopedEntity;
import com.infotact.warehouse.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence entity representing a customer outbound sales order.
 * <p>
 * This entity tracks the lifecycle of a sale from initial placement to
 * picking, packing, and final dispatch. It acts as the parent container
 * for individual {@link SellingOrderItem} line items.
 * </p>
 */
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_number", columnList = "orderNumber")
})
public class SellingOrder extends WarehouseScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Unique business-facing reference (e.g., ORD-2026-0001).
     * <p>
     * Logic: This is the primary identifier used by customer support
     * and printed on packing slips.
     * </p>
     */
    @Column(unique = true, nullable = false)
    private String orderNumber;

    /**
     * The current fulfillment state (e.g., PENDING, PICKING, SHIPPED).
     * <p>
     * State Transitions: Controlled via the OrderService to ensure
     * stock is reserved/deducted at the correct lifecycle stages.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * Timestamp of order creation.
     */
    private LocalDateTime createdAt;

    /**
     * Logic: The deadline for fulfillment. If CURRENT_TIMESTAMP exceeds this date
     * and the order isn't SHIPPED, it is flagged as DELAYED in the dashboard.
     * <p>
     * <b>Note:</b> Added to resolve the 'UnknownPathException' during startup.
     * </p>
     */
    private LocalDateTime expectedShipDate;

    /**
     * List of products and quantities included in this order.
     * <p>
     * Cascading: Uses {@link CascadeType#ALL} to ensure that order line
     * items are managed entirely through the parent SellingOrder lifecycle.
     * </p>
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<SellingOrderItem> items;
}