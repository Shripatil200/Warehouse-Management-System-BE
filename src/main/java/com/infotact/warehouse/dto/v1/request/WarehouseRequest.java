package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank(message = "Warehouse name cannot be empty")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @NotBlank(message = "Location is required")
        String location
) {}

