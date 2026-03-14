package com.infotact.warehouse.dto.v1.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    private String parentCategoryId; // Only send the ID of the parent
}
