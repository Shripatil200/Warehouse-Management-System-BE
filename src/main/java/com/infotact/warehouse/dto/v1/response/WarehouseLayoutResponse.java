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
 * It includes real-time aggregated metrics for capacity and occupancy at every structural level.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "WarehouseLayoutResponse",
        description = "Hierarchical model of the physical warehouse structure including maintenance status and capacity metrics"
)
public class WarehouseLayoutResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the warehouse facility", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered business name of the facility", example = "Central Hub Berlin")
    private String name;

    @Schema(description = "Total storage capacity across all zones in the warehouse", example = "10000")
    private Integer totalCapacity;

    @Schema(description = "Total units currently occupied across all zones", example = "4500")
    private Integer currentOccupancy;

    @Schema(description = "List of operational zones within the warehouse")
    private List<ZoneSummary> zones;

    /**
     * Summary of a specific functional area (e.g., Cold Storage).
     * <p>
     * Logic: Capacity and occupancy are aggregated from all aisles within this zone.
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Operational area details including maintenance status and aggregated zone capacity")
    public static class ZoneSummary {
        @Schema(description = "The unique UUID of the zone")
        private String id;

        @Schema(description = "The display name of the zone", example = "High-Value Goods")
        private String name;

        @Schema(description = "Indicates if the zone is active or closed for maintenance", example = "true")
        private boolean active;

        @Schema(description = "Sum of all aisle capacities in this zone")
        private Integer totalCapacity;

        @Schema(description = "Sum of all units stored in this zone")
        private Integer currentOccupancy;

        @Schema(description = "Set of physical storage aisles located within this zone")
        private Set<AisleSummary> aisles;
    }

    /**
     * Summary of a physical row/aisle within a warehouse zone.
     * <p>
     * Logic: Used to identify specific rows for picking or putaway.
     * Capacity metrics are aggregated from child bins.
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Storage aisle details including maintenance status and row-level capacity")
    public static class AisleSummary {
        @Schema(description = "The unique UUID of the aisle")
        private String id;

        @Schema(description = "The alphanumeric code identifying the aisle physically", example = "A-01")
        private String code;

        @Schema(description = "Indicates if the aisle is active or blocked", example = "true")
        private boolean active;

        @Schema(description = "Sum of all individual bin capacities in this aisle")
        private Integer totalCapacity;

        @Schema(description = "Sum of all units stored in this specific row")
        private Integer currentOccupancy;

        @Schema(description = "Set of specific storage bins/slots in this aisle")
        private Set<BinSummary> bins;
    }

    /**
     * Detailed view of a specific storage slot (Bin).
     * <p>
     * Logic: The leaf node of the hierarchy representing the final inventory destination.
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual storage bin details including individual occupancy")
    public static class BinSummary {
        @Schema(description = "The unique UUID of the storage bin")
        private String id;

        @Schema(description = "The unique location code for the bin", example = "BIN-A1-001")
        private String binCode;

        @Schema(description = "Maximum storage capacity (units) for this slot", example = "500")
        private Integer capacity;

        @Schema(description = "Current number of units stored in this bin", example = "120")
        private Integer currentOccupancy;

        @Schema(description = "Indicates if the specific bin is usable or under repair", example = "true")
        private boolean active;
    }
}