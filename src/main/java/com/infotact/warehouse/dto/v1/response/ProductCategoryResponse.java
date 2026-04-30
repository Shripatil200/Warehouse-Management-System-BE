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
 * This response provides a 360-degree view of a category, including its
 * relationship within the warehouse hierarchy, child categories for tree
 * visualization, and operational counts for reporting.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ProductCategoryResponse",
        description = "Hierarchical category model featuring multi-tenant isolation and UI metadata"
)
public class ProductCategoryResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the category", example = "a1b2c3d4-e5f6-7890")
    private String id;

    @Schema(description = "Display name of the category", example = "High-Value Electronics")
    private String name;

    @Schema(description = "Purpose and handling instructions", example = "Store in climate-controlled secure zone")
    private String description;

    @Schema(description = "Indicates if the category is currently available for use", example = "true")
    private boolean active;

    @Schema(description = "UUID of the warehouse-specific storage zone optimized for this category")
    private String preferredZoneId;

    // --- Hierarchy & Tree Visualization ---

    @Schema(description = "UUID of the parent category. Null if this is a root category.")
    private String parentCategoryId;

    @Schema(description = "Name of the parent category, useful for breadcrumbs", example = "Finished Goods")
    private String parentCategoryName;

    @Schema(description = "Flag indicating if this category has no parent", example = "true")
    private boolean isRoot;

    @Schema(description = "Recursive list of nested sub-categories")
    private List<ProductCategoryResponse> children;

    // --- Analytics & Operational Data ---

    @Schema(description = "The number of active products currently mapped to this category", example = "125")
    private Integer productCount;

    @Schema(description = "The number of direct sub-categories under this node", example = "4")
    private Integer subCategoryCount;

    // --- Audit Metadata ---

    @Schema(description = "Timestamp of category creation")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the last modification to category attributes")
    private LocalDateTime updatedAt;
}