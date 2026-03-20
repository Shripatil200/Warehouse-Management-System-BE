package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class WarehouseLayoutRequest {

    public record ZoneRequest(
            @Schema(description = "Unique name for the zone within the warehouse", example = "Cold Storage")
            @NotBlank(message = "Zone name is required")
            String name,

            @Schema(description = "UUID of the parent warehouse")
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {}

    public record AisleRequest(
            @Schema(description = "Alphanumeric code for the aisle", example = "A1")
            @NotBlank(message = "Aisle code is required")
            String code,

            @Schema(description = "UUID of the parent zone")
            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @Schema(description = "UUID of the parent warehouse for verification")
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {}

    public record BulkBinRequest(
            @NotBlank(message = "Warehouse ID is required")
            String warehouseId,

            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @NotBlank(message = "Aisle ID is required")
            String aisleId,

            @Schema(description = "Number of bins to generate in this batch (Max 100)", example = "50")
            @Min(1) @Max(100)
            int quantity,

            @Schema(description = "Text prefix for generated codes. Code will be {prefix}-{sequence}", example = "BIN-A1")
            @NotBlank(message = "Prefix is required")
            String prefix,

            @Schema(description = "Default capacity units for each bin", example = "500")
            @Positive
            Integer defaultCapacity
    ) {}
}