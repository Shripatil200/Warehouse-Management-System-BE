package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for manually triggering a settlement for a consignment agreement.
 * Used by MANAGER role via POST /api/v1/consignments/{agreementId}/settle
 */
@Data
public class TriggerSettlementRequest {

    @NotBlank(message = "Agreement ID is required")
    private String agreementId;

    @Size(max = 1000)
    private String managerNotes;
}
