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
 * Provides a 360-degree view of the catalog item, including logistics (dimensions),
 * financial valuation (cost price), and traceability flags (serialization/batching).
 * </p>
 */
@Data
@Builder
@Schema(
        name = "ProductResponse",
        description = "Full specification of a product including logistics and sourcing data"
)
public class ProductResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Internal unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered name of the product", example = "Industrial AC Motor")
    private String name;

    @Schema(description = "Stock Keeping Unit", example = "MOT-IND-001")
    private String sku;

    @Schema(description = "Detailed specifications", example = "3-Phase, 5HP High Torque Motor")
    private String description;

    // --- Financials ---

    @Schema(description = "Current unit sales price for customers", example = "12000.00")
    private BigDecimal sellingPrice;

    @Schema(description = "Purchase cost from supplier for valuation", example = "8500.00")
    private BigDecimal costPrice;

    // --- Logistics & Dimensions ---

    @Schema(description = "Unit of Measure", example = "PCS")
    private String uom;

    @Schema(description = "Weight in kilograms", example = "15.5")
    private Double weight;

    @Schema(description = "Length in cm", example = "40.0")
    private Double length;

    @Schema(description = "Width in cm", example = "30.0")
    private Double width;

    @Schema(description = "Height in cm", example = "30.0")
    private Double height;

    @Schema(description = "EAN/UPC barcode", example = "890123456789")
    private String barcode;

    // --- Operational Status ---

    @Schema(description = "Active status for catalog visibility", example = "true")
    private boolean active;

    @Schema(description = "Safety stock alert level", example = "5")
    private Integer minThreshold;

    @Schema(description = "Maximum storage limit to prevent overstocking", example = "100")
    private Integer maxThreshold;

    // --- Traceability ---

    @Schema(description = "Indicates if units are tracked by unique serial numbers")
    private boolean isSerialized;

    @Schema(description = "Indicates if items are tracked by batch/lot numbers")
    private boolean isBatchTracked;

    // --- Relationships ---

    @Schema(description = "Assigned category UUID", example = "c1b2c3d4-e5f6-7890")
    private String categoryId;

    @Schema(description = "Display name of the category", example = "Industrial Machinery")
    private String categoryName;

    @Schema(description = "Available sourcing and supplier options for this product")
    private List<ProductSupplierResponse> sourcingOptions;

    @Schema(description = "Timestamp of catalog creation")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last attribute update")
    private LocalDateTime updatedAt;
}