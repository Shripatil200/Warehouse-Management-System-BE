package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class InventorySummaryResponse {
    private String sku;
    private String productName;

    // Quantity Breakdown
    private Long totalPhysicalQuantity;
    private Long totalReservedQuantity;
    private Long totalAvailableQuantity;
    private Long uniqueLocationsCount;

    // Financials (Manager Level)
    private BigDecimal currentPurchasePrice;
    private BigDecimal currentSellingPrice;
    private BigDecimal totalInventoryValue; // (Total Physical * Current Purchase Price)
}