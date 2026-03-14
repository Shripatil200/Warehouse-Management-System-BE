package com.infotact.warehouse.dto.v1.response;

import lombok.Data;

import java.util.List;

@Data
public class ProductCategoryResponse {
    private String id;
    private String name;
    private String parentCategoryName;
    private List<ProductCategoryResponse> children;
    // Flattened for easier UI consumption
    // Note: We usually don't return the full list of products here
    // to keep the response lightweight.
}