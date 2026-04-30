package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for confirming the physical receipt of goods.
 * <p>
 * Transitions stock from 'Inbound PO' state to 'Available' Inventory.
 * <b>Update:</b> Added Expiry Date for FEFO tracking and Unit Cost for
 * precise inventory valuation.
 * </p>
 */
@Data
@Schema(
        name = "ReceivingRequest",
        description = "Payload used to record the physical arrival of products into a specific warehouse bin"
)
public class ReceivingRequest {

    @NotBlank(message = "Product ID is required")
    @Schema(
            description = "The internal UUID of the product being received",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String productId;

    @NotBlank(message = "Storage Bin ID is required")
    @Schema(
            description = "The UUID of the physical bin/shelf where items are placed",
            example = "bin-9922-x1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String storageBinId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(
            description = "The physical count of units received",
            example = "100",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer quantity;

    @NotBlank(message = "Batch number is required")
    @Schema(
            description = "Manufacturer or internal batch/lot identifier",
            example = "LOT-2026-04-A",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String batchNumber;

    @Schema(
            description = "Expiration date of the specific batch (Crucial for FEFO picking)",
            example = "2027-12-31"
    )
    private LocalDate expiryDate;

    @Schema(
            description = "The unit purchase price for this specific receipt. Defaults to product cost if null.",
            example = "850.50"
    )
    private BigDecimal unitCost;
}