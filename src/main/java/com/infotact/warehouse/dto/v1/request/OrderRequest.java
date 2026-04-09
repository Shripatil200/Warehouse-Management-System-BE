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
 * Data Transfer Object for creating a new outbound customer order.
 * <p>
 * This request captures the business order number and a list of specific products
 * to be picked and shipped from the warehouse. Validation ensures that the order
 * is not empty and quantities are positive.
 * </p>
 */
@Data
@Schema(description = "Request payload for creating a new outbound customer order")
public class OrderRequest {

    @NotBlank(message = "Order number is required")
    @Schema(
            description = "Unique business reference identifier for the order",
            example = "ORD-2026-001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String orderNumber;

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
                example = "AUDIO-SONY-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String sku;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Minimum order quantity is 1")
        @Schema(
                description = "The number of units to be shipped",
                example = "5",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer quantity;
    }
}