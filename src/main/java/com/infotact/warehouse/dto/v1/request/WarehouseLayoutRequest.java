package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Container for request records related to the physical configuration of a warehouse.
 * <p>
 * This class groups the requests for building the structural hierarchy:
 * Zones, Aisles, and Bins.
 * </p>
 */
@Schema(description = "Wrapper for warehouse structural configuration requests")
public class WarehouseLayoutRequest {

    /**
     * Request to define a new operational area within the warehouse.
     */
    @Schema(description = "Payload for creating a new warehouse zone")
    public record ZoneRequest(
            @Schema(description = "Unique name for the zone", example = "Cold Storage")
            @NotBlank(message = "Zone name is required")
            String name,

            @Schema(description = "The UUID of the parent warehouse facility")
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {}

    /**
     * Request to register a new aisle within a specific zone.
     */
    @Schema(description = "Payload for creating a new storage aisle")
    public record AisleRequest(
            @Schema(description = "Alphanumeric identifier for the aisle", example = "A1")
            @NotBlank(message = "Aisle code is required")
            String code,

            @Schema(description = "The UUID of the parent zone")
            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @Schema(description = "The UUID of the warehouse for cross-verification")
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {}

    /**
     * Request to automate the creation of multiple storage bins.
     * <p>
     * <b>Update:</b> Now requires both volumetric and weight limits to
     * initialize modern storage constraints.
     * </p>
     */
    @Schema(description = "Payload for bulk generating storage bins with physical constraints")
    public record BulkBinRequest(
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId,

            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @NotBlank(message = "Aisle ID is required")
            String aisleId,

            @Schema(description = "Total number of bins to generate (Max 100)", example = "50")
            @Min(1) @Max(100)
            int quantity,

            @Schema(description = "Naming prefix (e.g., BIN-A1 results in BIN-A1-001)", example = "BIN-A1")
            @NotBlank(message = "Prefix is required")
            String prefix,

            @Schema(description = "Max physical volume for each bin in cm³", example = "100000.0")
            @NotNull(message = "Default volume capacity is required")
            @Positive
            Double defaultMaxVolume,

            @Schema(description = "Max structural weight limit for each bin in KG", example = "500.0")
            @NotNull(message = "Default weight capacity is required")
            @Positive
            Double defaultMaxWeight
    ) {}

}