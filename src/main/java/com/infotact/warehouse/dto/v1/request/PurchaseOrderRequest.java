package com.infotact.warehouse.dto.v1.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PurchaseOrderRequest(
        @NotBlank(message = "Supplier ID is required")
        String supplierId,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        List<PurchaseOrderItemRequest> items
) {
    public record PurchaseOrderItemRequest(
            @NotBlank(message = "Product SKU is required")
            String sku,

            @NotNull(message = "Quantity is required")
            @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {
    }
}
