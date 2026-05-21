package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for a supplier adding or updating a product offering in their catalogue.
 */
@Data
@Schema(name = "SupplierProductRequest", description = "Payload for adding or updating a supplier product offering")
public class SupplierProductRequest {

    @NotBlank(message = "Product master ID is required")
    @Schema(description = "UUID of the ProductMaster this offering is for", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productMasterId;

    @NotNull(message = "Supply price is required")
    @DecimalMin(value = "0.0001", message = "Supply price must be positive")
    @Schema(description = "Unit price this supplier charges", example = "45.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal supplyPrice;

    @NotNull(message = "Lead time is required")
    @Min(value = 0, message = "Lead time cannot be negative")
    @Schema(description = "Expected delivery time in calendar days", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer leadTimeDays;
}
