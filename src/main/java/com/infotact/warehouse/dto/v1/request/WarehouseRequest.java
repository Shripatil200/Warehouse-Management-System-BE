package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register or update a warehouse facility")
public record WarehouseRequest(
        @Schema(description = "Official name of the facility", example = "Central Hub Berlin")
        @NotBlank(message = "Warehouse name cannot be empty")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @Schema(description = "Physical address or coordinates", example = "Alexanderplatz 1, Berlin")
        @NotBlank(message = "Location is required")
        String location
) {}