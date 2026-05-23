package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinRentalResponse {

    private String id;
    private String supplierId;
    private String supplierName;
    private String storageBinId;
    private String binCode;
    private BigDecimal rentalRatePerDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private String notes;
    private List<BinRentalPaymentResponse> payments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BinRentalPaymentResponse {
        private String id;
        private LocalDate periodFrom;
        private LocalDate periodTo;
        private Integer totalDays;
        private BigDecimal totalAmount;
        private String status;
        private String paidAt;
    }
}
