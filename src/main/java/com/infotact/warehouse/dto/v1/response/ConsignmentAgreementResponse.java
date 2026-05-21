package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.ConsignmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for a ConsignmentAgreement — returned by the API.
 */
@Data
@Builder
public class ConsignmentAgreementResponse {

    private String id;
    private String agreementCode;
    private String supplierId;
    private String supplierName;
    private BigDecimal warehouseCommissionPct;
    private ConsignmentStatus status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer settlementCycleDays;
    private String notes;
    private List<ConsignmentProductResponse> products;

    @Data
    @Builder
    public static class ConsignmentProductResponse {
        private String id;
        private String productId;
        private String productName;
        private String sku;
        private BigDecimal mrp;
        private BigDecimal floorPrice;
        private boolean active;
    }
}
