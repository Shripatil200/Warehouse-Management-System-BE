package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.ProductMaster;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a global product definition.
 */
@Data
@NoArgsConstructor
@Schema(name = "ProductMasterResponse", description = "Global product definition")
public class ProductMasterResponse {

    private String id;
    private String name;
    private String description;
    private String barcode;
    private String categoryId;
    private String categoryName;
    private String uom;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;

    public ProductMasterResponse(ProductMaster pm) {
        this.id           = pm.getId();
        this.name         = pm.getName();
        this.description  = pm.getDescription();
        this.barcode      = pm.getBarcode();
        this.uom          = pm.getUom();
        this.weight       = pm.getWeight();
        this.length       = pm.getLength();
        this.width        = pm.getWidth();
        this.height       = pm.getHeight();
        if (pm.getCategory() != null) {
            this.categoryId   = pm.getCategory().getId();
            this.categoryName = pm.getCategory().getName();
        }
    }
}
