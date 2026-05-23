package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Revenue and spend summary for a single supplier within a time period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRevenueResponse {

    private String supplierId;
    private String supplierName;

    /** Total consignment commission earned from this supplier's products. */
    private BigDecimal commissionEarned;

    /** Total bin rental revenue earned from this supplier. */
    private BigDecimal binRentalEarned;

    /** commissionEarned + binRentalEarned */
    private BigDecimal totalRevenueFromSupplier;

    /** Sum of (unitCost × quantity) on RECEIVED purchase orders from this supplier. */
    private BigDecimal totalSpentOnSupplier;

    /** totalRevenueFromSupplier − totalSpentOnSupplier (positive = net income). */
    private BigDecimal netPosition;
}
