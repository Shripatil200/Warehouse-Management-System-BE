package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for a supplier updating their own profile.
 * All fields are optional — only provided fields are updated.
 */
@Data
@Schema(name = "SupplierProfileUpdateRequest", description = "Payload for supplier profile self-update")
public class SupplierProfileUpdateRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Full name or contact person name", example = "Rajesh Kumar")
    private String name;

    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(description = "10-digit primary mobile number", example = "9876543210")
    private String contactNumber;

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    @Schema(description = "Registered business name", example = "Kumar Enterprises Pvt Ltd")
    private String companyName;

    @Schema(description = "GST or tax registration number", example = "27AAPFU0939F1ZV")
    private String gstNumber;

    @Schema(description = "Physical or billing address", example = "123, Industrial Area, Pune - 411001")
    private String address;

    @Schema(description = "Company website URL", example = "https://kumarenterprises.com")
    private String website;
}
