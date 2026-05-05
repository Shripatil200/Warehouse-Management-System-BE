package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.entity.enums.ZoneType;
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
 * It is optimized for a drill-down UI where structural metadata (Capacities and Counts)
 * is provided upfront, while granular data (Bins) can be lazy-loaded.
 * </p>
 * <p>
 * <b>Update Highlights:</b>
 * <ul>
 *     <li>Included <b>binCount</b> in AisleSummary to support UI badges without loading full bin lists.</li>
 *     <li>Maintains volumetric and weight-based occupancy metrics for real-time progress bars.</li>
 * </ul>
 * </p>
 *
 * @author Gemini
 * @version 3.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "WarehouseLayoutResponse",
        description = "Hierarchical model of the physical warehouse structure with volumetric occupancy and bin counts"
)
public class WarehouseLayoutResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the warehouse facility", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered business name of the facility", example = "Central Hub Berlin")
    private String name;

    @Schema(description = "Total volumetric capacity (cm³) across all zones", example = "1000000.0")
    private Double totalCapacity;

    @Schema(description = "Total volumetric units occupied across all zones", example = "450000.0")
    private Double currentOccupancy;

    @Schema(description = "List of operational zones within the warehouse")
    private List<ZoneSummary> zones;

    /**
     * Summary of a specific functional area (e.g., Cold Storage, Inbound).
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

        @Schema(description = "The functional role of this zone", example = "BULK")
        private ZoneType zoneType;

        @Schema(description = "Indicates if the zone is active and available for operations", example = "true")
        private boolean active;

        @Schema(description = "Sum of all aisle capacities in this zone (cm³)")
        private Double totalCapacity;

        @Schema(description = "Sum of all occupied volume in this zone (cm³)")
        private Double currentOccupancy;

        @Schema(description = "Set of physical storage aisles within this zone")
        private Set<AisleSummary> aisles;
    }

    /**
     * Summary of a physical row or aisle.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Storage aisle details with row-level metrics and bin counts")
    public static class AisleSummary {
        @Schema(description = "The unique UUID of the aisle")
        private String id;

        @Schema(description = "The alphanumeric identifier for the aisle", example = "A-01")
        private String code;

        @Schema(description = "Indicates if the aisle is currently operational", example = "true")
        private boolean active;

        @Schema(description = "Sum of all bin capacities in this aisle (cm³)")
        private Double totalCapacity;

        @Schema(description = "Sum of all occupied volume in this specific row (cm³)")
        private Double currentOccupancy;

        /**
         * Total number of storage bins assigned to this aisle.
         * Used to drive UI badges and determine if pagination is needed.
         */
        @Schema(description = "Total number of physical storage bins located in this aisle", example = "48")
        private Integer binCount;

        @Schema(description = "Set of specific storage bins. May be empty if using lazy-loading (drill-down) pattern.")
        private Set<BinSummary> bins;
    }

    /**
     * Detailed view of a specific storage slot (Bin).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual storage bin details including volumetric and weight load percentages")
    public static class BinSummary {
        @Schema(description = "The unique UUID of the storage bin")
        private String id;

        @Schema(description = "The unique location code for labeling and scanning", example = "BIN-A1-001")
        private String binCode;

        @Schema(description = "The physical format of the bin", example = "PICK_FACE")
        private BinType binType;

        @Schema(description = "Maximum physical volume of the bin (cm³)", example = "50000.0")
        private Double capacity;

        @Schema(description = "Current volume occupied by inventory (cm³)", example = "12000.0")
        private Double currentOccupancy;

        @Schema(description = "Maximum weight limit the bin can safely hold (KG)", example = "500.0")
        private Double maxWeight;

        @Schema(description = "Current weight load based on stored items (KG)", example = "45.5")
        private Double currentWeight;

        @Schema(description = "The higher of Volume vs Weight utilization for UI progress bars (0-100)", example = "75")
        private Integer fillPercentage;

        @Schema(description = "Indicates if the bin is active and usable for storage", example = "true")
        private boolean active;
    }
}