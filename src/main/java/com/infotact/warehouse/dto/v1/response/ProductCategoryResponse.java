package com.infotact.warehouse.dto.v1.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductCategoryResponse {
    private String id;
    private String name;
    private boolean active; // To support Soft Delete [cite: 32, 156-158]
    private String parentCategoryName;
    private List<ProductCategoryResponse> children;

    // Auditing fields for Enterprise tracking [cite: 137-140]
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}