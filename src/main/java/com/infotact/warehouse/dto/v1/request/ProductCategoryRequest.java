package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for creating or updating a product category.
 * <p>
 * Supports a hierarchical structure. If {@code parentCategoryId} is null,
 * the category is treated as a root/top-level category. If provided,
 * it becomes a child of the specified parent.
 * </p>
 */
@Data
@Schema(
        name = "ProductCategoryRequest",
        description = "Payload for managing hierarchical product categories"
)
public class ProductCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Schema(
            description = "Unique name of the category within the warehouse context",
            example = "Electronics",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(
            description = "The UUID of the parent category. Leave null for top-level categories.",
            example = "550e8400-e29b-41d4-a716-446655440000",
            nullable = true
    )
    private String parentCategoryId;
}