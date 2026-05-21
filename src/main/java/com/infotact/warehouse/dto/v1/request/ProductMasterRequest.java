package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for creating or updating a {@link com.infotact.warehouse.entity.ProductMaster} record.
 */
@Data
@Schema(name = "ProductMasterRequest", description = "Payload for creating or updating a global product definition")
public class ProductMasterRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Universal product name", example = "AA Alkaline Battery 1.5V", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed product description", example = "Standard 1.5V AA alkaline battery, 2400mAh capacity")
    private String description;

    @Schema(description = "Industry-standard barcode (EAN-13, UPC-A, etc.)", example = "5901234123457")
    private String barcode;

    @Schema(description = "UUID of the product category", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String categoryId;

    @NotBlank(message = "Unit of measure is required")
    @Schema(description = "Unit of measure (e.g., PCS, BOX, KG)", example = "PCS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uom;

    @Schema(description = "Weight in kilograms", example = "0.025")
    private Double weight;

    @Schema(description = "Length in centimetres", example = "5.0")
    private Double length;

    @Schema(description = "Width in centimetres", example = "1.5")
    private Double width;

    @Schema(description = "Height in centimetres", example = "1.5")
    private Double height;
}
