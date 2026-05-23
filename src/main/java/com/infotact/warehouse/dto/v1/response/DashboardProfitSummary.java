package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Profit and revenue summary block for the management dashboard.
 * Broken down into thisMonth, lastMonth, and thisYear windows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardProfitSummary {

    private ProfitBlock thisMonth;
    private ProfitBlock lastMonth;
    private ProfitBlock thisYear;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitBlock {

        /** Profit from warehouse-owned product sales: (sellPrice − costPrice) × qty. */
        private BigDecimal ownedProfit;

        /** Commission income from consignment sales (= warehouseShare). */
        private BigDecimal consignmentProfit;

        /** ownedProfit + consignmentProfit. */
        private BigDecimal totalProfit;

        /** Total revenue from owned product sales (sellPrice × qty). */
        private BigDecimal totalRevenue;

        /** Bin rental income from consignment suppliers. */
        private BigDecimal binRentalRevenue;
    }
}
