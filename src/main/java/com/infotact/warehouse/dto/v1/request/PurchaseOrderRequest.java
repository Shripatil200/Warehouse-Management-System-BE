package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for creating an inbound Purchase Order.
 * <p>
 * This request records the intent to receive stock from a specific supplier.
 * <b>Update:</b> Now captures the unitCost for each item to ensure the correct
 * purchase price is locked in for the incoming batch.
 * </p>
 */
@Schema(description = "Payload for creating a new inbound purchase order with specific batch pricing")
public record PurchaseOrderRequest(
        @NotBlank(message = "Supplier ID is required")
        @Schema(description = "The UUID of the supplier providing the goods",
                example = "sup-8821-b321", requiredMode = Schema.RequiredMode.REQUIRED)
        String supplierId,

        @NotBlank(message = "Target Warehouse ID is required")
        @Schema(description = "The UUID of the destination warehouse",
                example = "wh-550e-8400", requiredMode = Schema.RequiredMode.REQUIRED)
        String warehouseId,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        @Schema(description = "List of products, quantities, and agreed costs")
        List<PurchaseOrderItemRequest> items
) {
    /**
     * Inner record representing an individual line item in the Purchase Order.
     */
    @Schema(description = "Individual product line item including cost capture")
    public record PurchaseOrderItemRequest(
            @NotBlank(message = "Product SKU is required")
            @Schema(description = "The unique SKU of the item",
                    example = "APP-IP17-256-BLK", requiredMode = Schema.RequiredMode.REQUIRED)
            String sku,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Schema(description = "Number of units ordered", example = "50", minimum = "1")
            Integer quantity,

            @NotNull(message = "Unit cost is required")
            @Positive(message = "Unit cost must be positive")
            @Schema(description = "The negotiated purchase price per unit (e.g., 10.00 or 12.00)",
                    example = "72000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal unitCost
    ) {
    }
}