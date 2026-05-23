package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight sourcing option embedded inside {@link ProductResponse}.
 * Shows which suppliers can provide a product and at what price.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProductSupplierResponse", description = "Lightweight sourcing option shown inside a product response")
public class ProductSupplierResponse {

    @Schema(description = "Supplier's contact person name or company name")
    private String supplierName;

    @Schema(description = "Unit price this supplier charges", example = "45.50")
    private BigDecimal supplyPrice;

    @Schema(description = "Expected delivery time in calendar days", example = "7")
    private Integer leadTimeDays;
}
