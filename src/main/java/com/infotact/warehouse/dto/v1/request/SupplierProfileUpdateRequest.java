package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "SupplierProfileUpdateRequest", description = "Supplier profile self-update — all fields optional")
public class SupplierProfileUpdateRequest {

    @Size(min = 2, max = 100)
    @Schema(example = "Rajesh Kumar")
    private String name;

    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(example = "9876543210")
    private String contactNumber;

    @Size(max = 200)
    @Schema(example = "Kumar Enterprises Pvt Ltd")
    private String companyName;

    @Schema(example = "27AAPFU0939F1ZV")
    private String gstNumber;

    @Schema(example = "123, Industrial Area, Pune - 411001")
    private String address;

    @Schema(example = "https://kumarenterprises.com")
    private String website;
}
