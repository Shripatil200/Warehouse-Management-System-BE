package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Pricing and lead time details from a specific supplier")
public class ProductSupplierResponse {

    @Schema(description = "Supplier name", example = "Apple India")
    private String supplierName;

    @Schema(description = "The cost price offered by this supplier", example = "72000.00")
    private BigDecimal currentSupplyPrice;

    @Schema(description = "Days required for delivery", example = "3")
    private Integer leadTimeDays;
}