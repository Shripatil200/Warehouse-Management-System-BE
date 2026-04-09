package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Data Transfer Object for creating an inbound Purchase Order.
 * <p>
 * This request records the intent to receive stock from a specific supplier
 * into a target warehouse facility. It serves as the legal/operational
 * document used during the 'Receiving' process.
 * </p>
 */
@Schema(description = "Payload for creating a new inbound purchase order from a supplier")
public record PurchaseOrderRequest(
        @NotBlank(message = "Supplier ID is required")
        @Schema(description = "The UUID of the supplier providing the goods",
                example = "sup-8821-b321", requiredMode = Schema.RequiredMode.REQUIRED)
        String supplierId,

        @NotBlank(message = "Target Warehouse ID is required")
        @Schema(description = "The UUID of the destination warehouse where stock will be stored",
                example = "wh-550e-8400", requiredMode = Schema.RequiredMode.REQUIRED)
        String warehouseId,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        @Schema(description = "List of products and quantities expected from the supplier")
        List<PurchaseOrderItemRequest> items
) {
    /**
     * Inner record representing an individual line item in the Purchase Order.
     */
    @Schema(description = "Individual product line item for the purchase order")
    public record PurchaseOrderItemRequest(
            @NotBlank(message = "Product SKU is required")
            @Schema(description = "The unique Stock Keeping Unit (SKU) of the item",
                    example = "ELEC-LAP-DELL-01", requiredMode = Schema.RequiredMode.REQUIRED)
            String sku,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Schema(description = "The number of units ordered from the supplier",
                    example = "50", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Integer quantity
    ) {
    }
}