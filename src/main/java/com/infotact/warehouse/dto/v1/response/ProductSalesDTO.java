package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing sales and financial performance.
 * <p>
 * Aggregates volume, revenue, and profit.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ProductSalesDTO",
        description = "Aggregated model for performance rankings and financial analytics"
)
public class ProductSalesDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The display name of the product", example = "iPhone 17 256GB")
    private String productName;

    @Schema(description = "Total units sold", example = "150")
    private Long salesCount;

    @Schema(description = "Total revenue generated (Gross Sales)", example = "11999850.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Net profit (Total Revenue - Total Batch Costs)", example = "2250000.00")
    private BigDecimal totalProfit;

    @Schema(description = "Profit margin percentage", example = "18.75")
    private Double profitMarginPercentage;

    /**
     * Custom Constructor for JPQL "Top Selling" Query.
     * <p>
     * This constructor matches the signature: (String, Long).
     * It is required because Hibernate's 'SELECT new' syntax calls constructors
     * by parameter matching, ignoring Lombok's default All-Args constructor
     * which now expects financial fields.
     * </p>
     */
    public ProductSalesDTO(String productName, Long salesCount) {
        this.productName = productName;
        this.salesCount = salesCount;
        this.totalRevenue = BigDecimal.ZERO;
        this.totalProfit = BigDecimal.ZERO;
        this.profitMarginPercentage = 0.0;
    }
}