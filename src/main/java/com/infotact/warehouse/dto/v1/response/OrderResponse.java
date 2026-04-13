package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the detailed state of an outbound customer order.
 * <p>
 * This response includes denormalized data (Warehouse and Product names) to optimize
 * frontend rendering performance. It provides a complete snapshot of the order's
 * lifecycle and its constituent line items.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "OrderResponse",
        description = "Detailed view of an outbound order including fulfillment status and itemized details"
)
public class OrderResponse  implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The internal unique identifier (UUID) of the order",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The business-facing order reference number",
            example = "ORD-2026-001")
    private String orderNumber;

    @Schema(description = "The current fulfillment state of the order",
            example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Timestamp when the order was initially created")
    private LocalDateTime createdAt;

    @Schema(description = "Name of the warehouse processing this order (Denormalized)",
            example = "Central Hub Berlin")
    private String warehouseName;

    @Schema(description = "List of specific products and quantities included in the order")
    private List<OrderItemDetail> items;

    /**
     * Detail view of a single product within an outbound order.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detailed information for a single order line item")
    public static class OrderItemDetail {

        @Schema(description = "The internal UUID of the product",
                example = "p1b2c3d4-e5f6-7890")
        private String productId;

        @Schema(description = "The display name of the product (Denormalized)",
                example = "Sony WH-1000XM4")
        private String productName;

        @Schema(description = "The Stock Keeping Unit (SKU) for warehouse picking",
                example = "AUDIO-SNY-4000")
        private String sku;

        @Schema(description = "Number of units ordered",
                example = "2")
        private Integer quantity;
    }
}