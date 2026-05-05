package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object providing a full specification of a product.
 * <p>
 * Includes cached volumetric data and stock health status indicators
 * to optimize frontend performance and dashboard rendering.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ProductResponse",
        description = "Complete product profile including logistics and sourcing metadata"
)
public class ProductResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Internal unique identifier (UUID)")
    private String id;

    @Schema(description = "The registered name of the product")
    private String name;

    @Schema(description = "Warehouse-unique Stock Keeping Unit")
    private String sku;

    @Schema(description = "Detailed specifications")
    private String description;

    // --- Financials ---

    private BigDecimal sellingPrice;
    private BigDecimal costPrice;

    // --- Logistics & Pre-calculated Footprint ---

    private String uom;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;

    @Schema(description = "Calculated unit volume (cm³). Used for storage bin calculations.", example = "36000.0")
    private BigDecimal unitVolume;

    private String barcode;

    // --- Operational Status & Health ---

    @Schema(description = "Active status for catalog visibility")
    private boolean active;

    @Schema(description = "True if current stock is at or below minThreshold", example = "false")
    private boolean isLowStock;

    private Integer minThreshold;
    private Integer maxThreshold;
    private Integer minReplenishThreshold;
    private Integer maxPickFaceCapacity;

    // --- Traceability ---

    private boolean isSerialized;
    private boolean isBatchTracked;

    // --- Relationships & Metadata ---

    private String categoryId;
    private String categoryName;

    @Schema(description = "List of approved suppliers and their current pricing quotes")
    private List<ProductSupplierResponse> sourcingOptions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "True if any PICK_FACE bin is below minReplenishThreshold")
    private boolean needsReplenishment;
}