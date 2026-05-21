package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public supplier profile, safe to expose via API.
 */
@Data
@NoArgsConstructor
@Schema(name = "SupplierResponse", description = "Supplier profile information")
public class SupplierResponse {

    @Schema(description = "Supplier user UUID")
    private String id;

    @Schema(description = "Contact person name")
    private String name;

    @Schema(description = "Business email")
    private String email;

    @Schema(description = "Primary contact number")
    private String contactNumber;

    @Schema(description = "Registered business name")
    private String companyName;

    @Schema(description = "GST or tax registration number")
    private String gstNumber;

    @Schema(description = "Physical or billing address")
    private String address;

    @Schema(description = "Company website URL")
    private String website;

    public SupplierResponse(User user) {
        this.id            = user.getId();
        this.name          = user.getName();
        this.email         = user.getEmail();
        this.contactNumber = user.getContactNumber();
        this.companyName   = user.getCompanyName();
        this.gstNumber     = user.getGstNumber();
        this.address       = user.getAddress();
        this.website       = user.getWebsite();
    }
}
