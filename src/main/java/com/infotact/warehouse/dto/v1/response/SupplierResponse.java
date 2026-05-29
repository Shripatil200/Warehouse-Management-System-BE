package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.ProductCategory;
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
    private java.util.List<String> categoryIds;
    private java.util.List<String> categoryNames;

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
        
        this.categoryIds = supplier.getCategories() != null
            ? supplier.getCategories().stream().map(ProductCategory::getId).collect(java.util.stream.Collectors.toList())
            : new java.util.ArrayList<>();
            
        this.categoryNames = supplier.getCategories() != null
            ? supplier.getCategories().stream().map(ProductCategory::getName).collect(java.util.stream.Collectors.toList())
            : new java.util.ArrayList<>();
    }
}
