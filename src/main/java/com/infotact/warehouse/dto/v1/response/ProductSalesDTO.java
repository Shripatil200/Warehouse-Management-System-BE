package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object representing sales performance for a specific product.
 * <p>
 * This DTO is used to populate dashboard rankings and sales analytics charts.
 * It provides an aggregated count of units sold across all fulfilled orders.
 * </p>
 */
@Data
@AllArgsConstructor
@Schema(
        name = "ProductSalesDTO",
        description = "Simplified model for product performance rankings and sales volume charts"
)
public class ProductSalesDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(
            description = "The display name of the product",
            example = "Sony WH-1000XM4"
    )
    private String productName;

    @Schema(
            description = "The total number of units sold/dispatched for this product",
            example = "150"
    )
    private Long salesCount;
}