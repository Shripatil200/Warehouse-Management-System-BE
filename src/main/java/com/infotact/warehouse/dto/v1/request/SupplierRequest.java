package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request payload for creating or updating a supplier record.
 * Suppliers are managed by warehouse Admins/Managers.
 */
@Data
@Schema(name = "SupplierRequest", description = "Payload for creating or updating a supplier")
public class SupplierRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    @Schema(example = "Rajesh Kumar", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "rajesh@supplier.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactNumber;

    @NotBlank(message = "Company name is required")
    @Size(max = 200)
    @Schema(example = "Kumar Enterprises Pvt Ltd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @Schema(example = "27AAPFU0939F1ZV")
    private String gstNumber;

    @Schema(example = "123, Industrial Area, Pune - 411001")
    private String address;

    @Schema(example = "https://kumarenterprises.com")
    private String website;

    private java.util.List<String> categoryIds = new java.util.ArrayList<>();
}
