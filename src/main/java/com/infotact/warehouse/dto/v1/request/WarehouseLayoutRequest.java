package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Container for request records related to the physical configuration of a warehouse.
 * <p>
 * This class groups the requests for building the structural hierarchy:
 * Zones (Areas), Aisles (Rows), and Bins (Specific storage slots).
 * </p>
 */
@Schema(description = "Wrapper for warehouse structural configuration requests")
public class WarehouseLayoutRequest {

    /**
     * Request to define a new operational area within the warehouse.
     */
    @Schema(description = "Payload for creating a new warehouse zone")
    public record ZoneRequest(
            @Schema(description = "Unique name for the zone (e.g., Cold Storage, Receiving)",
                    example = "Cold Storage", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Zone name is required")
            String name,

            @Schema(description = "The UUID of the parent warehouse facility",
                    example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {}

    /**
     * Request to register a new aisle (row) within a specific zone.
     */
    @Schema(description = "Payload for creating a new storage aisle")
    public record AisleRequest(
            @Schema(description = "Alphanumeric identifier for the aisle",
                    example = "A1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Aisle code is required")
            String code,

            @Schema(description = "The UUID of the parent zone",
                    example = "z1b2c3d4-e5f6-7890", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @Schema(description = "The UUID of the warehouse for cross-verification",
                    example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {}

    /**
     * Request to automate the creation of multiple storage bins.
     */
    @Schema(description = "Payload for bulk generating storage bins within an aisle")
    public record BulkBinRequest(
            @Schema(description = "Target Warehouse UUID", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId,

            @Schema(description = "Target Zone UUID", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @Schema(description = "Target Aisle UUID", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Aisle ID is required")
            String aisleId,

            @Schema(description = "Total number of bins to generate in this operation (Max 100)",
                    example = "50", minimum = "1", maximum = "100")
            @Min(1) @Max(100)
            int quantity,

            @Schema(description = "Naming prefix. Generated bins will follow: {prefix}-001, {prefix}-002, etc.",
                    example = "BIN-A1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Prefix is required")
            String prefix,

            @Schema(description = "The maximum capacity/volume limit for each generated bin",
                    example = "500", minimum = "1")
            @Positive
            Integer defaultCapacity
    ) {}
}