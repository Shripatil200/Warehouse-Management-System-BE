package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object for creating or updating a product in the master catalog.
 * <p>
 * This request captures all physical and commercial attributes of an item.
 * Product validation ensures SKU uniqueness and mandatory category association.
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
            example = "Sony WH-1000XM4 Wireless Headphones",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "SKU must be uppercase alphanumeric")
    @Schema(
            description = "Stock Keeping Unit - must be unique and uppercase alphanumeric",
            example = "AUDIO-SNY-4000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String sku;

    @Schema(
            description = "Detailed product description and specifications",
            example = "Industry-leading noise canceling overhead headphones with Mic"
    )
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price cannot be negative")
    @Schema(
            description = "Unit sales price of the product",
            example = "349.99",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal price;

    @Positive(message = "Weight must be positive")
    @Schema(description = "Physical weight of the unit in kilograms", example = "0.25")
    private Double weight;

    @Schema(description = "Universal barcode or EAN number", example = "4548736112148")
    private String barcode;

    @Min(value = 0, message = "Threshold cannot be negative")
    @Schema(
            description = "Minimum stock quantity before the system triggers a low-stock alert",
            example = "10"
    )
    private Integer minThreshold;

    @NotBlank(message = "Category ID is required")
    @Schema(
            description = "The UUID of the assigned product category",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String categoryId;
}