package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for creating or updating a product category.
 * <p>
 * Includes support for hierarchical parenting and warehouse-specific
 * optimization hints like 'preferredZoneId'.
 * </p>
 */
@Data
@Schema(
        name = "ProductCategoryRequest",
        description = "Payload for managing hierarchical and zone-optimized categories"
)
public class ProductCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100)
    @Schema(description = "Name of the classification", example = "High-Value Electronics")
    private String name;

    @Schema(description = "Detailed purpose of this category", example = "Requires specialized handling and secure storage")
    private String description;

    @Schema(
            description = "UUID of the parent category. Null for root categories.",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String parentCategoryId;

    @Schema(
            description = "Hint for put-away logic: UUID of the ideal storage zone",
            example = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
    )
    private String preferredZoneId;
}