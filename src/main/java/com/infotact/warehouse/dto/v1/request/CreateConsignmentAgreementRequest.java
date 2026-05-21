package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a new ConsignmentAgreement.
 * Submitted by a MANAGER on behalf of or with the supplier.
 */
@Data
public class CreateConsignmentAgreementRequest {

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    /**
     * Warehouse commission percentage (0–100).
     */
    @NotNull
    @DecimalMin(value = "0.00", message = "Commission cannot be negative")
    @DecimalMax(value = "100.00", message = "Commission cannot exceed 100%")
    private BigDecimal warehouseCommissionPct;

    @NotNull(message = "Effective-from date is required")
    private LocalDate effectiveFrom;

    /** Null = open-ended agreement */
    private LocalDate effectiveTo;

    /** Days between automatic settlement runs. Default: 30 */
    @Min(value = 1, message = "Settlement cycle must be at least 1 day")
    private Integer settlementCycleDays = 30;

    @Size(max = 2000)
    private String notes;

    @NotEmpty(message = "At least one product must be included in the agreement")
    @Valid
    private List<ConsignmentProductRequest> products;

    @Data
    public static class ConsignmentProductRequest {

        @NotBlank
        private String productId;

        @NotNull
        @DecimalMin("0.00")
        private BigDecimal mrp;

        /** Optional floor price (minimum selling price) */
        @DecimalMin("0.00")
        private BigDecimal floorPrice;
    }
}
