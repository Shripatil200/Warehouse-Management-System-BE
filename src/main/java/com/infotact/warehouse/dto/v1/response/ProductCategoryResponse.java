package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing a product category and its hierarchy.
 * <p>
 * Features warehouse optimization hints like 'preferredZoneId' and supports
 * recursive nesting for tree-view visualization.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ProductCategoryResponse",
        description = "Hierarchical category model featuring zone optimization hints"
)
public class ProductCategoryResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the category")
    private String id;

    @Schema(description = "Display name", example = "Flammable Chemicals")
    private String name;

    @Schema(description = "Purpose and handling instructions", example = "Store in fire-proof zone")
    private String description;

    @Schema(description = "Indicates if the category is currently active")
    private boolean active;

    @Schema(description = "UUID of the preferred storage zone for this category",
            example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private String preferredZoneId;

    @Schema(description = "UUID of the parent category", example = "550e8400-e29b-41d4-a716-446655440000")
    private String parentCategoryId;

    @Schema(description = "Name of the parent category for breadcrumbs", example = "Raw Materials")
    private String parentCategoryName;

    @Schema(description = "List of nested sub-categories")
    private List<ProductCategoryResponse> children;

    @Schema(description = "Number of active products in this category", example = "42")
    private Integer productCount;

    @Schema(description = "Timestamp of category creation")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of last modification")
    private LocalDateTime updatedAt;
}