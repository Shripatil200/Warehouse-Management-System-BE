package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class WarehouseLayoutRequest {


    public record ZoneRequest(
            @NotBlank(message = "Zone name is required")
            String name,

            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {
    }

    public record AisleRequest(
            @NotBlank(message = "Aisle code is required")
            String code,

            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @NotBlank(message = "Warehouse ID is required")
            String warehouseId
    ) {
    }


    public record BulkBinRequest(

            @NotBlank(message = "Warehouse ID is required")
            String warehouseId,

            @NotBlank(message = "Zone ID is required")
            String zoneId,

            @NotBlank(message = "Aisle ID is required")
            String aisleId,

            @Min(1) @Max(100)
            int quantity, // How many bins to create

            @NotBlank(message = "Prefix is required")
            String prefix, // e.g., "BIN-A1"

            @Positive
            Integer defaultCapacity // e.g., 500
    ) {
    }

}