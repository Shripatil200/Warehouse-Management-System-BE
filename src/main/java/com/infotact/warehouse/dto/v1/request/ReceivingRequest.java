package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for confirming the physical receipt of goods.
 * <p>
 * This request is used to update the actual inventory levels in the warehouse.
 * It transition stock from 'In-Transit' (Purchase Order state) to
 * 'Available' (Inventory state).
 * </p>
 */
@Data
@Schema(
        name = "ReceivingRequest",
        description = "Payload used to record the physical arrival of products into the warehouse"
)
public class ReceivingRequest {

    @NotBlank(message = "Product ID is required")
    @Schema(
            description = "The internal UUID of the product being received",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(
            description = "The physical count of units received and verified",
            example = "100",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer quantity;
}