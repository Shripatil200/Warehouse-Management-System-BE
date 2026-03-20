package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request for creating or updating a product category")
public class ProductCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Schema(description = "Unique name of the category", example = "Electronics")
    private String name;

    @Schema(description = "Optional UUID of the parent category for hierarchy", example = "550e8400-e29b-41d4-a716-446655440000")
    private String parentCategoryId;
}