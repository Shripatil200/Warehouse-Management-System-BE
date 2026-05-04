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
 * Industry-Ready: Includes physical picking directions (Bin Codes) and inventory layer
 * tracking to bridge the digital order to the physical warehouse floor.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "OrderResponse",
        description = "Detailed view of an outbound order including financial snapshots and physical picking directions"
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

    @Schema(description = "List of specific products and picking directions for the order")
    private List<OrderItemDetail> items;

    /**
     * Detail view of a single product within an outbound order.
     * Updated to support mobile scan verification and physical picking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detailed information for an order line item including physical storage locations")
    public static class OrderItemDetail {

        @Schema(description = "The internal UUID of the product",
                example = "p1b2c3d4-e5f6-7890")
        private String productId;

        @Schema(description = "The display name of the product",
                example = "iPhone 17 256GB")
        private String productName;

        @Schema(description = "The Stock Keeping Unit (SKU) for picking stickers",
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

        // --- INDUSTRY-READY PICKING DIRECTIONS ---

        @Schema(description = "The human-readable code on the physical rack for the picker",
                example = "ZONE-A-AISLE-01-BIN-005")
        private String binCode;

        @Schema(description = "The internal UUID of the suggested bin for verification scans",
                example = "bin_uuid_123")
        private String suggestedBinId;

        @Schema(description = "The specific inventory layer (batch/expiry) reserved for this pick",
                example = "inv_item_uuid_456")
        private String inventoryItemId;
    }
}