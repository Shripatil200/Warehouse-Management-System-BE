package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object representing a system-generated operational alert.
 * <p>
 * Alerts are triggered by inventory thresholds, order delays, or supply chain
 * interruptions. They are intended to be displayed on the management dashboard
 * for immediate action.
 * </p>
 */
@Data
@AllArgsConstructor
@Schema(
        name = "AlertDTO",
        description = "Notification model for critical warehouse operational events"
)
public class AlertDTO {

    @Schema(
            description = "A human-readable description of the alert",
            example = "Product 'Sony Headphones' is below the minimum threshold (5 units remaining)."
    )
    private String message;

    @Schema(
            description = "The classification of the alert which determines priority and UI styling",
            example = "LOW_STOCK",
            allowableValues = {"LOW_STOCK", "OUT_OF_STOCK", "DELAYED", "SYSTEM_ERROR"}
    )
    private String type;
}