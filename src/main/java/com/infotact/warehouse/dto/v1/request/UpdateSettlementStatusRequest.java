package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for approving or marking a settlement as PAID.
 * Used by MANAGER via PATCH /api/v1/consignments/settlements/{settlementId}/approve
 * and PATCH /api/v1/consignments/settlements/{settlementId}/mark-paid
 */
@Data
public class UpdateSettlementStatusRequest {

    @Size(max = 1000)
    private String managerNotes;
}
