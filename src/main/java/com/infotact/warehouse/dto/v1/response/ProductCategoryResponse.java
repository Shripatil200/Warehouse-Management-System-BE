package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing a product category and its position in the hierarchy.
 * <p>
 * This response supports a recursive structure, enabling the retrieval of
 * nested sub-categories. It includes denormalized parent names for
 * breadcrumb navigation and auditing timestamps for lifecycle tracking.
 * </p>
 */
@Data
@Schema(
        name = "ProductCategoryResponse",
        description = "Response model for product categories featuring hierarchical nesting"
)
public class ProductCategoryResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique UUID of the category",
            example = "c1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "The display name of the category",
            example = "Consumer Electronics")
    private String name;

    @Schema(description = "Indicates if the category is active. Inactive categories are hidden from the catalog.",
            example = "true")
    private boolean active;

    @Schema(description = "Name of the immediate parent category (Denormalized)",
            example = "Retail Goods")
    private String parentCategoryName;

    @Schema(description = "List of sub-categories nested under this category")
    private List<ProductCategoryResponse> children;

    @Schema(description = "The timestamp when the category was first created")
    private LocalDateTime createdAt;

    @Schema(description = "The timestamp of the last modification to this category")
    private LocalDateTime updatedAt;
}