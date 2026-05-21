package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for supplier self-registration.
 * Suppliers create their own accounts independently of any warehouse.
 */
@Data
@Schema(name = "SupplierRegistrationRequest", description = "Payload for supplier self-registration")
public class SupplierRegistrationRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Full name or contact person name", example = "Rajesh Kumar", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Business email used for login and PO notifications", example = "rajesh@supplier.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(description = "10-digit primary mobile number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Account password (min 8 characters)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    @Schema(description = "Registered business name", example = "Kumar Enterprises Pvt Ltd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @Schema(description = "GST or tax registration number", example = "27AAPFU0939F1ZV")
    private String gstNumber;

    @Schema(description = "Physical or billing address", example = "123, Industrial Area, Pune - 411001")
    private String address;

    @Schema(description = "Company website URL", example = "https://kumarenterprises.com")
    private String website;
}
