package com.infotact.warehouse.dto.v1.response;

import lombok.Data;

@Data
public class ProductCategoryResponse {
    private String id;
    private String name;
    private String parentCategoryName; // Flattened for easier UI consumption
    // Note: We usually don't return the full list of products here
    // to keep the response lightweight.
}