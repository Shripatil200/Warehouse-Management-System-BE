package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the product category lifecycle.
 * <p>
 * This service manages the organizational hierarchy used for cataloging products.
 * It ensures data integrity within the category tree and enforces multi-tenant
 * isolation at the warehouse level.
 * </p>
 */
public interface CategoryService {

    /**
     * Creates a new product category within a specific warehouse context.
     * <p>
     * <b>Validation Logic:</b>
     * 1. Unique Name Check: Verifies no category with the same name exists in the current warehouse.
     * 2. Parent Validation: If a parent ID is provided, it must exist and belong to the same warehouse.
     * </p>
     * @param request Data containing the name, description, and optional parent category.
     * @return The persisted category mapped to a response DTO.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if name collision occurs.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if parent ID is invalid.
     */
    ProductCategoryResponse addCategory(ProductCategoryRequest request);

    /**
     * Retrieves a specific category.
     * @param id The UUID of the category.
     * @return Detailed category information.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if ID does not exist.
     */
    ProductCategoryResponse getCategory(String id);

    /**
     * Retrieves a paginated list of categories filtered by facility and status.
     * <p>
     * <b>Multi-tenancy:</b> This list is strictly filtered by the logged-in user's warehouse ID.
     * </p>
     * @param pageable Pagination and sorting parameters.
     * @param includeInactive If true, includes "Soft-Deleted" categories.
     * @return A pageable collection of categories.
     */
    Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive);

    /**
     * Permanently removes a category from the database.
     * <p>
     * <b>Safety Guardrails:</b>
     * 1. Child Check: Blocks deletion if the category has active sub-categories.
     * 2. Product Check: Blocks deletion if any products are currently assigned to this category.
     * </p>
     * @param id The UUID of the category to remove.
     * @throws com.infotact.warehouse.exception.IllegalOperationException if the category is not "Empty".
     */
    void deleteCategory(String id);

    /**
     * Updates an existing category's metadata or position in the hierarchy.
     * <p>
     * <b>Circular Reference Protection:</b>
     * Prevents logic errors where a category is set as its own parent or as a
     * descendant of its own child, which would break tree traversal.
     * </p>
     * @param id Unique identifier of the target category.
     * @param request The updated attributes.
     * @return The updated category details.
     */
    ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request);

    /**
     * Performs a "Soft-Delete" by marking the category as inactive.
     * <p>
     * Logic: Inactive categories remain in the database for historical audit
     * purposes but are filtered out of operational dropdowns and picker views.
     * </p>
     */
    ProductCategoryResponse deactivateCategory(String id);

    /**
     * Restores an inactive category to operational status.
     */
    ProductCategoryResponse activateCategory(String id);
}