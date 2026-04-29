package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object for creating or updating a product in the master catalog.
 * <p>
 * This request captures all physical and commercial attributes of an item.
 * <b>Update:</b> Validates the sellingPrice as the primary revenue value,
 * distinct from batch-specific purchase costs.
 * </p>
 */
@Data
@Schema(
        name = "ProductRequest",
        description = "Payload for registering or modifying product details in the catalog"
)
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    @Schema(
            description = "Display name of the product",
            example = "iPhone 17 256GB",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "SKU must be uppercase alphanumeric")
    @Schema(
            description = "Stock Keeping Unit - unique business identifier used for tracking",
            example = "APP-IP17-256-BLK",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String sku;

    @Schema(
            description = "Detailed product description and specifications",
            example = "Apple iPhone 17 with 256GB Storage, Black Titanium"
    )
    private String description;

    @NotNull(message = "Selling price is required")
    @PositiveOrZero(message = "Selling price cannot be negative")
    @Schema(
            description = "The target price for customer sales (Revenue)",
            example = "79999.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal sellingPrice;

    @Positive(message = "Weight must be positive")
    @Schema(description = "Physical weight of the unit in kilograms", example = "0.18")
    private Double weight;

    @Schema(description = "Universal barcode (EAN/UPC) for scanning", example = "194253123456")
    private String barcode;

    @Min(value = 0, message = "Threshold cannot be negative")
    @Schema(
            description = "Inventory level that triggers a 'Low Stock' dashboard alert",
            example = "5"
    )
    private Integer minThreshold;

    @NotBlank(message = "Category ID is required")
    @Schema(
            description = "UUID of the product category for hierarchical grouping",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String categoryId;
}