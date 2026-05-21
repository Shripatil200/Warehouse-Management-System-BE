package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.ConsignmentSettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a single ConsignmentSettlement record.
 */
@Data
@Builder
public class ConsignmentSettlementResponse {

    private String id;
    private String settlementNumber;
    private String agreementId;
    private String agreementCode;
    private String supplierName;

    private LocalDate periodFrom;
    private LocalDate periodTo;

    private BigDecimal totalGrossRevenue;
    private BigDecimal totalWarehouseShare;
    private BigDecimal totalSupplierPayout;
    private Integer totalUnitsSold;

    private ConsignmentSettlementStatus status;
    private String managerNotes;
    private LocalDateTime paidAt;

    /** Summary breakdown per product within this settlement */
    private List<ProductBreakdown> productBreakdowns;

    @Data
    @Builder
    public static class ProductBreakdown {
        private String productId;
        private String productName;
        private Integer unitsSold;
        private BigDecimal grossRevenue;
        private BigDecimal warehouseShare;
        private BigDecimal supplierShare;
    }
}
