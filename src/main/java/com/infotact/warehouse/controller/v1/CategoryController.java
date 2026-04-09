package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing product categories within the warehouse.
 * <p>
 * This controller manages a hierarchical category tree, allowing for
 * organizational depth (parent/child relationships).
 * All operations are restricted to users with the <b>MANAGER</b> role.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "2. Product Categories", description = "Endpoints for managing the hierarchical product category tree")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Creates a new product category.
     * <p>
     * Logic: Accepts a unique name and an optional parent category ID.
     * If parentId is provided, the new category becomes a sub-category.
     * </p>
     *
     * @param request Data containing category name and optional parent reference.
     * @return The created category details.
     */
    @Operation(summary = "Create a new category", description = "Creates a top-level or sub-category. Names must be unique within the warehouse.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or parent ID provided"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Access restricted to Managers"),
            @ApiResponse(responseCode = "409", description = "Conflict: Category name already exists")
    })
    @PostMapping
    public ResponseEntity<ProductCategoryResponse> addCategory(@Valid @RequestBody ProductCategoryRequest request) {
        log.info("REST request to add category: {}", request.getName());
        return new ResponseEntity<>(categoryService.addCategory(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves a single category by its UUID.
     *
     * @param id The unique identifier of the category.
     * @return The category details.
     */
    @Operation(summary = "Get category by ID", description = "Retrieves full details of a specific category, including its parent reference.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> getCategory(
            @Parameter(description = "The UUID of the category", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    /**
     * Lists all categories available to the manager's warehouse.
     *
     * @param pageable Pagination and sorting information.
     * @param includeInactive Flag to determine if soft-deleted categories should be returned.
     * @return A paginated list of categories.
     */
    @Operation(summary = "List all categories", description = "Returns a paginated list of categories mapped to the current warehouse.")
    @GetMapping
    public ResponseEntity<Page<ProductCategoryResponse>> getAllCategories(
            @ParameterObject Pageable pageable,
            @Parameter(description = "If true, returns both active and inactive categories")
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        log.info("REST request to get all categories. includeInactive={}", includeInactive);
        return ResponseEntity.ok(categoryService.getAllCategories(pageable, includeInactive));
    }

    /**
     * Permanently removes a category from the database.
     * <p>
     * Logic: Deletion is only permitted if the category has no sub-categories
     * and is not linked to any products.
     * </p>
     *
     * @param id The unique identifier of the category to be deleted.
     */
    @Operation(summary = "Delete a category", description = "Hard delete for categories. Fails if dependencies (sub-categories or products) exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request: Category has dependencies and cannot be removed")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        log.warn("REST request to delete category: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates an existing category's details.
     * <p>
     * Logic: Allows changing the name or parent category.
     * Circular dependency checks are performed at the service level.
     * </p>
     */
    @Operation(summary = "Update a category", description = "Updates category name or parent reference.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody ProductCategoryRequest request) {

        log.info("REST request to update category: {}", id);
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "Activate a category", description = "Sets category status to active, making it visible for product assignment.")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductCategoryResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.activateCategory(id));
    }

    @Operation(summary = "Deactivate a category", description = "Sets category status to inactive (Soft Delete).")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductCategoryResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.deactivateCategory(id));
    }
}