package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Data Transfer Object for registering or modifying products in the master catalog.
 * <p>
 * This payload captures physical specs (dimensions/weight) required for automated
 * put-away logic and financial data required for inventory valuation.
 * </p>
 */
@Data
@Schema(
        name = "ProductRequest",
        description = "Comprehensive payload for product lifecycle management"
)
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    @Schema(description = "Display name of the product", example = "Industrial AC Motor")
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "SKU must be uppercase alphanumeric (dashes/underscores allowed)")
    @Schema(description = "Unique Stock Keeping Unit scoped to the warehouse", example = "MOT-IND-001")
    private String sku;

    @Schema(description = "Detailed technical specifications and notes", example = "3-Phase, 5HP High Torque Motor")
    private String description;

    // --- Financials ---

    @NotNull(message = "Selling price is required")
    @PositiveOrZero(message = "Selling price cannot be negative")
    @Schema(description = "Revenue price charged to customers", example = "12000.00")
    private BigDecimal sellingPrice;

    @NotNull(message = "Cost price is required")
    @PositiveOrZero(message = "Cost price cannot be negative")
    @Schema(description = "Purchase cost from supplier used for warehouse valuation", example = "8500.00")
    private BigDecimal costPrice;

    // --- Logistics & Dimensions ---

    @NotBlank(message = "Unit of Measure (UOM) is required")
    @Schema(description = "Standard unit of measurement", example = "PCS", allowableValues = {"PCS", "BOX", "KG", "LTR"})
    private String uom;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    @Schema(description = "Weight in Kilograms (KG) per unit", example = "15.5")
    private Double weight;

    @NotNull(message = "Length is required")
    @Positive(message = "Length must be positive")
    @Schema(description = "Physical length in CM", example = "40.0")
    private Double length;

    @NotNull(message = "Width is required")
    @Positive(message = "Width must be positive")
    @Schema(description = "Physical width in CM", example = "30.0")
    private Double width;

    @NotNull(message = "Height is required")
    @Positive(message = "Height must be positive")
    @Schema(description = "Physical height in CM", example = "30.0")
    private Double height;

    @Schema(description = "Universal barcode (EAN/UPC/GTIN)", example = "890123456789")
    private String barcode;

    // --- Inventory Controls ---

    @Min(value = 0)
    @Schema(description = "Safety stock level. Triggers replenishment alerts.", example = "10")
    private Integer minThreshold = 5;

    @Schema(description = "Maximum storage limit to prevent overstocking", example = "100")
    private Integer maxThreshold;

    // --- Traceability Flags ---

    @Schema(description = "If true, every unit requires a unique serial number", example = "false")
    private boolean isSerialized;

    @Schema(description = "If true, items are tracked by Batch/Lot numbers", example = "false")
    private boolean isBatchTracked;

    @NotBlank(message = "Category ID is required")
    @Schema(description = "UUID of the associated product category")
    private String categoryId;
}