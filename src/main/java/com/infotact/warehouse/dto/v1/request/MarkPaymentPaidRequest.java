package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarkPaymentPaidRequest {

    @NotBlank(message = "Payment ID is required")
    private String paymentId;
}
