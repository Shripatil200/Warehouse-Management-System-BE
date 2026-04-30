package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Data Transfer Object representing the complete physical hierarchy of a warehouse.
 * <p>
 * This response follows a parent-child nesting pattern: <b>Warehouse -> Zones -> Aisles -> Bins</b>.
 * <b>Update:</b> Now includes volumetric and weight-based occupancy metrics to support
 * real-time dashboard progress bars.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "WarehouseLayoutResponse",
        description = "Hierarchical model of the physical warehouse structure with volumetric occupancy metrics"
)
public class WarehouseLayoutResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the warehouse facility", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered business name of the facility", example = "Central Hub Berlin")
    private String name;

    @Schema(description = "Total volumetric capacity (cm³) across all zones", example = "1000000")
    private Double totalCapacity;

    @Schema(description = "Total volumetric units occupied across all zones", example = "450000")
    private Double currentOccupancy;

    @Schema(description = "List of operational zones within the warehouse")
    private List<ZoneSummary> zones;

    /**
     * Summary of a specific functional area.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Operational area details with aggregated volumetric data")
    public static class ZoneSummary {
        @Schema(description = "The unique UUID of the zone")
        private String id;

        @Schema(description = "The display name of the zone", example = "High-Value Goods")
        private String name;

        @Schema(description = "Indicates if the zone is active", example = "true")
        private boolean active;

        @Schema(description = "Sum of all aisle volumes in this zone (cm³)")
        private Double totalCapacity;

        @Schema(description = "Sum of all occupied volume in this zone (cm³)")
        private Double currentOccupancy;

        @Schema(description = "Set of physical storage aisles within this zone")
        private Set<AisleSummary> aisles;
    }

    /**
     * Summary of a physical row/aisle.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Storage aisle details with row-level volumetric occupancy")
    public static class AisleSummary {
        @Schema(description = "The unique UUID of the aisle")
        private String id;

        @Schema(description = "The alphanumeric identifier", example = "A-01")
        private String code;

        @Schema(description = "Indicates if the aisle is active", example = "true")
        private boolean active;

        @Schema(description = "Sum of all bin volumes in this aisle (cm³)")
        private Double totalCapacity;

        @Schema(description = "Sum of all occupied volume in this row (cm³)")
        private Double currentOccupancy;

        @Schema(description = "Set of specific storage bins in this aisle")
        private Set<BinSummary> bins;
    }

    /**
     * Detailed view of a specific storage slot (Bin).
     * <p>
     * <b>Update:</b> Added fillPercentage to drive dashboard progress bars and
     * weightLoad metrics for secondary capacity tooltips.
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual storage bin details including volumetric and weight load percentages")
    public static class BinSummary {
        @Schema(description = "The unique UUID of the storage bin")
        private String id;

        @Schema(description = "The unique location code", example = "BIN-A1-001")
        private String binCode;

        @Schema(description = "Max physical volume (cm³)", example = "50000")
        private Double capacity;

        @Schema(description = "Current volume occupied (cm³)", example = "12000")
        private Double currentOccupancy;

        @Schema(description = "Max weight limit (KG)", example = "500.0")
        private Double maxWeight;

        @Schema(description = "Current weight load (KG)", example = "45.5")
        private Double currentWeight;

        @Schema(description = "The highest utilization ratio (Volume vs Weight) for UI progress bars", example = "75")
        private Integer fillPercentage;

        @Schema(description = "Indicates if the bin is usable", example = "true")
        private boolean active;
    }
}