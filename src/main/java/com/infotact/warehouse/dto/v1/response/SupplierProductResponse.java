package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.SupplierProduct;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SupplierProductResponse", description = "A supplier's product offering with pricing and lead time")
public class SupplierProductResponse {

    private String id;
    private String productMasterId;
    private String productMasterName;
    private String supplierId;
    private String supplierName;
    private String supplierCompanyName;
    private BigDecimal supplyPrice;
    private Integer leadTimeDays;
    private boolean active;

    public SupplierProductResponse(SupplierProduct sp) {
        this.id                  = sp.getId();
        this.productMasterId     = sp.getProductMaster().getId();
        this.productMasterName   = sp.getProductMaster().getName();
        this.supplierId          = sp.getSupplier().getId();
        this.supplierName        = sp.getSupplier().getName();
        this.supplierCompanyName = sp.getSupplier().getCompanyName();
        this.supplyPrice         = sp.getSupplyPrice();
        this.leadTimeDays        = sp.getLeadTimeDays();
        this.active              = sp.isActive();
    }
}
