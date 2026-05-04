package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class InventoryAdjustmentRequest {
    @NotBlank(message = "Inventory Item ID is required")
    private String inventoryItemId;

    @NotNull(message = "Adjustment quantity is required")
    private Integer adjustmentQuantity; // Positive to add, negative to remove

    @NotBlank(message = "Reason code is required (e.g., DAMAGED, CYCLE_COUNT)")
    private String reasonCode;
}