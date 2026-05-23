package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(name = "SupplierResponse", description = "Supplier public profile")
public class SupplierResponse {

    private String id;
    private String name;
    private String email;
    private String contactNumber;
    private String companyName;
    private String gstNumber;
    private String address;
    private String website;
    private SupplierStatus status;

    public SupplierResponse(Supplier supplier) {
        this.id            = supplier.getId();
        this.name          = supplier.getName();
        this.email         = supplier.getEmail();
        this.contactNumber = supplier.getContactNumber();
        this.companyName   = supplier.getCompanyName();
        this.gstNumber     = supplier.getGstNumber();
        this.address       = supplier.getAddress();
        this.website       = supplier.getWebsite();
        this.status        = supplier.getStatus();
    }
}
