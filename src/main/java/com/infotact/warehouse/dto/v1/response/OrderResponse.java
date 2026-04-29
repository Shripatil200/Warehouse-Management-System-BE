package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the detailed state of an outbound customer order.
 * <p>
 * This response provides a financial and operational snapshot of a sale.
 * It includes denormalized names and price snapshots to ensure
 * historical data integrity even if the master catalog prices change.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "OrderResponse",
        description = "Detailed view of an outbound order including financial snapshots and fulfillment status"
)
public class OrderResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The internal unique identifier (UUID) of the order",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The business-facing order reference number",
            example = "ORD-2026-101")
    private String orderNumber;

    @Schema(description = "The current fulfillment state of the order",
            example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Timestamp when the order was initially created")
    private LocalDateTime createdAt;

    @Schema(description = "Total monetary value of the order", example = "159998.00")
    private BigDecimal totalAmount;

    @Schema(description = "Name of the warehouse processing this order",
            example = "Pune Main Warehouse")
    private String warehouseName;

    @Schema(description = "List of specific products and quantities included in the order")
    private List<OrderItemDetail> items;

    /**
     * Detail view of a single product within an outbound order.
     * Includes the price snapshot for historical accuracy.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detailed information for a single order line item including price snapshots")
    public static class OrderItemDetail {

        @Schema(description = "The internal UUID of the product",
                example = "p1b2c3d4-e5f6-7890")
        private String productId;

        @Schema(description = "The display name of the product",
                example = "iPhone 17 256GB")
        private String productName;

        @Schema(description = "The Stock Keeping Unit (SKU) for picking",
                example = "APP-IP17-256-BLK")
        private String sku;

        @Schema(description = "Number of units ordered", example = "2")
        private Integer quantity;

        @Schema(description = "The price per unit at the time the order was placed",
                example = "79999.00")
        private BigDecimal sellPriceAtTimeOfOrder;

        @Schema(description = "Line item total (Quantity * Sell Price)",
                example = "159998.00")
        private BigDecimal lineTotal;
    }
}