package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerificationRequest {

    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier; // This will hold either the email or the phone number

    @NotBlank(message = "OTP is required")
    private String otp;
}
