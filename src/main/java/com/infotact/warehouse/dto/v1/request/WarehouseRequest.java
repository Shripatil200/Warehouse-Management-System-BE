package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for managing existing warehouse facility metadata.
 * <p>
 * This request is used for administrative updates to a warehouse's
 * physical attributes. Security Note: Only users with the <b>ADMIN</b>
 * role can initiate these changes.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "WarehouseRequest",
        description = "Payload for updating existing warehouse facility details"
)
public class WarehouseRequest {

    @Schema(
            description = "The registered business name of the facility",
            example = "Infotact Central Hub - Berlin",
            minLength = 3,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Warehouse name cannot be empty")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Schema(
            description = "The physical street address or geographic coordinates of the warehouse",
            example = "Alexanderplatz 1, 10178 Berlin, Germany",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Location is required")
    private String location;
}