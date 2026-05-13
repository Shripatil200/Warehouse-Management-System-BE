package com.infotact.warehouse.dto.v1.request;

import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.entity.enums.ZoneType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Container for request records related to the physical configuration of a warehouse.
 * <p>
 * Updated to include Zone and Bin typing for advanced Putaway and Replenishment logic.
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

            @Schema(description = "Functional purpose of the zone", example = "BULK")
            @NotNull(message = "Zone type is required")
            ZoneType zoneType// Added for industrial logic[cite: 1]

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
            String zoneId
    ) {}

    /**
     * Request to automate the creation of multiple storage bins.
     */
    @Schema(description = "Payload for bulk generating storage bins with physical and functional constraints")
    public record BulkBinRequest(

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

            @Schema(description = "Optional: Override the default bin type inherited from the Zone", example = "PICK_FACE")
            BinType binTypeOverride, // Added for granular control[cite: 1]

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