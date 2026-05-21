package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for per-product profit breakdown by period.
 *
 * NOTE: totalUnitsSold is Long because JPQL SUM() always returns Long,
 * and Hibernate's constructor expression requires an exact type match.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductProfitResponse {

    private String productId;
    private String productName;
    private String sku;

    /** Week / month number depending on query context; 0 for yearly or summary. */
    private Integer period;
    private Integer year;

    /** Long because JPQL SUM(integer) resolves to Long at runtime. */
    private Long totalUnitsSold;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;

    /**
     * {@code true}  → consigned product (profit = warehouseShare / commission).
     * {@code false} → warehouse-owned product (profit = revenue − cost).
     */
    private boolean consignment;
}