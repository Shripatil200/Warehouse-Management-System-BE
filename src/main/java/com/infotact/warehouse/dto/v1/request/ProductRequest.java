package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "SKU must be uppercase alphanumeric")
    private String sku;

    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price cannot be negative")
    private BigDecimal price;

    @Positive(message = "Weight must be positive")
    private Double weight;

    private String barcode;

    @NotBlank(message = "Category ID is required")
    private String categoryId;
}