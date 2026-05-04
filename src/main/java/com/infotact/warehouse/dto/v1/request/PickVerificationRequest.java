package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Secure DTO for mobile scanner verification.
 * <p>
 * Bridges the physical barcode scan at the rack to the digital inventory layer.
 * This ensures 100% picking accuracy by validating the Location, Product, and specific Batch.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for hardware scanner verification during picking")
public class PickVerificationRequest {

    @NotBlank(message = "Order ID is required")
    @Schema(description = "UUID of the order being fulfilled", example = "ord_789")
    private String orderId;

    @NotBlank(message = "Inventory Item ID is required")
    @Schema(description = "The specific stock layer UUID reserved for this pick", example = "inv_layer_001")
    private String inventoryItemId;

    @NotBlank(message = "Physical bin scan required")
    @Schema(description = "Raw barcode text from the bin label", example = "ZONE-A-01-005")
    private String scannedBinCode;

    @NotBlank(message = "Physical product scan required")
    @Schema(description = "Raw barcode text from the product SKU sticker", example = "PROD-123-BLK")
    private String scannedProductSku;

    @NotNull(message = "Picked quantity must be specified")
    @Positive(message = "Quantity must be greater than zero")
    @Schema(description = "Total units physically removed from the bin", example = "5")
    private Integer quantity;
}