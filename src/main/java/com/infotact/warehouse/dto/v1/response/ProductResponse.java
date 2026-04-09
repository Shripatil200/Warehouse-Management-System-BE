package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a comprehensive view of a product.
 * <p>
 * This response is the primary model for product listings and detail views.
 * It includes denormalized category information and audit timestamps to support
 * enterprise-level reporting and UI breadcrumbs.
 * </p>
 */
@Data
@Builder
@Schema(
        name = "ProductResponse",
        description = "Full specification of a product in the warehouse catalog"
)
public class ProductResponse {

    @Schema(description = "Internal unique identifier (UUID) of the product",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered name of the product",
            example = "Sony WH-1000XM4 Wireless Headphones")
    private String name;

    @Schema(description = "Stock Keeping Unit - unique business identifier",
            example = "AUDIO-SNY-4000")
    private String sku;

    @Schema(description = "Detailed marketing and technical description",
            example = "Premium noise-canceling overhead headphones.")
    private String description;

    @Schema(description = "Current unit sales price", example = "349.99")
    private BigDecimal price;

    @Schema(description = "Physical weight per unit in kilograms", example = "0.25")
    private Double weight;

    @Schema(description = "EAN/UPC barcode number for scanning", example = "4548736112148")
    private String barcode;

    @Schema(description = "Operational status. Inactive products are soft-deleted.", example = "true")
    private boolean active;

    @Schema(description = "Safety stock level that triggers replenishment alerts", example = "10")
    private Integer minThreshold;

    @Schema(description = "UUID of the assigned category",
            example = "c1b2c3d4-e5f6-7890")
    private String categoryId;

    @Schema(description = "Name of the assigned category (Denormalized)",
            example = "Audio & Sound")
    private String categoryName;

    @Schema(description = "Timestamp of initial catalog entry")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last update to product attributes")
    private LocalDateTime updatedAt;
}