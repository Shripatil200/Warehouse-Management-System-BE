package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing a comprehensive view of a product.
 * <p>
 * This response provides a 360-degree view of the catalog item, including
 * denormalized category data and associated sourcing options (suppliers).
 * </p>
 */
@Data
@Builder
@Schema(
        name = "ProductResponse",
        description = "Full specification of a product including associated suppliers"
)
public class ProductResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Internal unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered name of the product", example = "iPhone 17 256GB")
    private String name;

    @Schema(description = "Stock Keeping Unit", example = "APP-IP17-256-BLK")
    private String sku;

    @Schema(description = "Detailed specifications", example = "256GB Storage, 8GB RAM")
    private String description;

    @Schema(description = "Current unit sales price for customers", example = "79999.00")
    private BigDecimal sellingPrice;

    @Schema(description = "Weight in kilograms", example = "0.18")
    private Double weight;

    @Schema(description = "EAN/UPC barcode", example = "194253123456")
    private String barcode;

    @Schema(description = "Active status for catalog visibility", example = "true")
    private boolean active;

    @Schema(description = "Safety stock alert level", example = "5")
    private Integer minThreshold;

    @Schema(description = "Assigned category UUID", example = "c1b2c3d4-e5f6-7890")
    private String categoryId;

    @Schema(description = "Display name of the category", example = "Smartphones")
    private String categoryName;

    /**
     * List of suppliers who provide this product.
     * <p>
     * Logic: Allows the manager to compare purchase costs (unitCost)
     * across different vendors directly from the product view.
     * </p>
     */
    @Schema(description = "Available sourcing and supplier options for this product")
    private List<ProductSupplierResponse> sourcingOptions;

    @Schema(description = "Timestamp of catalog creation")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last attribute update")
    private LocalDateTime updatedAt;
}