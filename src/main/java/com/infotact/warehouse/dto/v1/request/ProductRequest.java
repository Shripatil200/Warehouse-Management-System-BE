package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Request for adding or updating a specific product")
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    @Schema(description = "Commercial name of the product", example = "Sony WH-1000XM4")
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "SKU must be uppercase alphanumeric")
    @Schema(description = "Unique Stock Keeping Unit. Allowed: A-Z, 0-9, _, -", example = "AUDIO-SONY-001")
    private String sku;

    @Schema(description = "Detailed product information", example = "Noise cancelling headphones")
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price cannot be negative")
    @Schema(description = "Unit price in the default currency", example = "349.99")
    private BigDecimal price;

    @Positive(message = "Weight must be positive")
    @Schema(description = "Weight in grams", example = "254.0")
    private Double weight;

    @Schema(description = "Universal barcode or EAN", example = "4548736112100")
    private String barcode;

    @NotBlank(message = "Category ID is required")
    @Schema(description = "UUID of the assigned category")
    private String categoryId;
}