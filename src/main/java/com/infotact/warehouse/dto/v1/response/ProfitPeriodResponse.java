package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for warehouse-wide profit broken down by period (week / month / year).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitPeriodResponse {

    /** Week number (1–53) for WEEKLY, month number (1–12) for MONTHLY, 1 for YEARLY. */
    private Integer period;

    /** The year this record belongs to. */
    private Integer year;

    /** Total revenue from warehouse-owned product sales (sellPrice × qty). */
    private BigDecimal ownedRevenue;

    /** Total profit from warehouse-owned products: (sellPrice − costPrice) × qty. */
    private BigDecimal ownedProfit;

    /** Total commission revenue earned from consigned products (= warehouseShare). */
    private BigDecimal consignmentRevenue;

    /** Total profit from consignment (= consignmentRevenue; warehouse has no cost). */
    private BigDecimal consignmentProfit;

    /** ownedProfit + consignmentProfit. */
    private BigDecimal totalProfit;
}
