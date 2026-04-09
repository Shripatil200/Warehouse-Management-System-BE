package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a summary of a warehouse facility.
 * <p>
 * This response provides the top-level metadata for a warehouse, including
 * its operational status and a summary of its internal complexity (Zone Count).
 * It is primarily used for administrative listings and the initial dashboard load.
 * </p>
 */
@Data
@Builder
@Schema(
        name = "WarehouseResponse",
        description = "Metadata response for a warehouse facility"
)
public class WarehouseResponse {

    @Schema(description = "The internal unique identifier (UUID) of the facility",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "The registered business name of the warehouse",
            example = "Infotact Central Hub - Berlin")
    private String name;

    @Schema(description = "Physical address or geographic location of the building",
            example = "Alexanderplatz 1, Berlin, Germany")
    private String location;

    @Schema(description = "Operational state of the warehouse. Inactive warehouses block all user access.",
            example = "true")
    private boolean active;

    @Schema(description = "The timestamp when the facility was registered in the system")
    private LocalDateTime createdAt;

    @Schema(description = "The timestamp of the last administrative update to the facility details")
    private LocalDateTime updatedAt;

    @Schema(description = "The total number of operational zones defined within this facility",
            example = "5")
    private int zoneCount;
}