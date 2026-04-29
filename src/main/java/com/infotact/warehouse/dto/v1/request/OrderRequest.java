package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for creating a new outbound customer sales order.
 * <p>
 * This request captures the business order number, the source warehouse,
 * and a list of specific products to be picked and shipped.
 * </p>
 * <p>
 * <b>Update:</b> Linked to the SellingOrder entity logic, ensuring that
 * stock is reserved at the point of request.
 * </p>
 */
@Data
@Schema(description = "Request payload for creating a new outbound customer sales order")
public class OrderRequest {

    @NotBlank(message = "Order number is required")
    @Schema(
            description = "Unique business reference identifier for the order",
            example = "ORD-2026-101",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String orderNumber;

    @NotBlank(message = "Warehouse ID is required")
    @Schema(
            description = "The UUID of the warehouse that will fulfill this order",
            example = "w1b2c3d4-e5f6-7890",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String warehouseId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @Schema(description = "List of products and quantities to be included in this order")
    private List<OrderItemRequest> items;

    /**
     * Inner DTO representing a single line item within an outbound order.
     */
    @Data
    @Schema(description = "Details of an individual product item within the order")
    public static class OrderItemRequest {

        @NotBlank(message = "Product SKU is required")
        @Schema(
                description = "The unique Stock Keeping Unit (SKU) of the product",
                example = "APP-IP17-256-BLK",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String sku;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Minimum order quantity is 1")
        @Schema(
                description = "The number of units to be shipped",
                example = "2",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer quantity;
    }
}