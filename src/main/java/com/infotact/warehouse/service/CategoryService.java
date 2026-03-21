package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


/**
 * Service interface for managing the product category lifecycle.
 * Handles the organizational hierarchy of products within the warehouse.
 */
public interface CategoryService {


    /**
     * Creates a new product category.
     *
     * @param request Data containing the name and optional parent category ID.
     * @return The created category details.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException    if a category with the same name already exists.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the specified parent category ID does not exist.
     */
    public ProductCategoryResponse addCategory(ProductCategoryRequest request);

    /**
     * Retrieves a specific category by its unique identifier.
     *
     * @param id The UUID of the category.
     * @return The found category details.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if no category is found with the given ID.
     */
    public ProductCategoryResponse getCategory(String id);

    /**
     * Retrieves a paginated list of categories.
     *
     * @param pageable        Pagination and sorting information (page number, size, sort order).
     * @param includeInactive If true, returns all categories; if false, filters for active categories only.
     * @return A page of category response objects.
     */
    Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive);

    /**
     * Permanently removes category from the system.
     *
     * @param id The unique identifier of the category to delete.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the category does not exist.
     * @throws com.infotact.warehouse.exception.IllegalOperationException if category has existing sub-category or linked product.
     */
    void deleteCategory(String id);

    /**
     * Updates details of existing category.
     *
     * @param id      Unique identifier of category to update.
     * @param request The updated date (name, parent ID).
     * @return The updated category details.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException If category does not exist.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException    If the new name is already taken by another category.
     * @throws com.infotact.warehouse.exception.IllegalOperationException If update would create circular reference (e.g. self-parenting).
     */
    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request);

    /**
     * Set a category's status to inactive.
     * Inactive categories are generally hidden from standard operational views.
     *
     * @param id Unique identifier of category.
     * @return The updated category details with active set to false.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException If the category is not found.
     */
    ProductCategoryResponse deactivateCategory(String id);

    /**
     * Re-active previously deactivated category.
     *
     * @param id Unique identifier of category.
     * @return Updated category details with active set to true.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException If the category is not found.
     */
    ProductCategoryResponse activateCategory(String id);

}
