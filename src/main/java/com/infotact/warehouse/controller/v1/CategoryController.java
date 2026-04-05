package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST controller for managing product categories.
 * Restricted strictly to users with the MANAGER role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')") // Restricts the entire controller to Managers
@Tag(name = "Product Categories", description = "Endpoints for managing the hierarchical product category tree")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Create a new category", description = "Creates a top-level or sub-category. Names must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or parent ID"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only Managers can create categories"),
            @ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    @PostMapping
    public ResponseEntity<ProductCategoryResponse> addCategory(@Valid @RequestBody ProductCategoryRequest request) {
        log.info("REST request to add category: {}", request.getName());
        return new ResponseEntity<>(categoryService.addCategory(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get category by ID", description = "Retrieves details of a specific category including its active status.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> getCategory(
            @Parameter(description = "The UUID of the category") @PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @Operation(summary = "List all categories", description = "Returns a paginated list of categories. Can filter by active status.")
    @GetMapping
    public ResponseEntity<Page<ProductCategoryResponse>> getAllCategories(
            @ParameterObject Pageable pageable,
            @Parameter(description = "If true, returns both active and inactive categories")
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        log.info("REST request to get all categories. includeInactive={}", includeInactive);
        return ResponseEntity.ok(categoryService.getAllCategories(pageable, includeInactive));
    }

    @Operation(summary = "Delete a category", description = "Permanently removes a category. Fails if it contains sub-categories or products.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Category has dependencies and cannot be deleted")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        log.warn("REST request to delete category: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a category", description = "Updates name or parent reference. Prevents circular dependencies.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody ProductCategoryRequest request) {

        log.info("REST request to update category: {}", id);
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "Activate a category", description = "Sets the category status to active.")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductCategoryResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.activateCategory(id));
    }

    @Operation(summary = "Deactivate a category", description = "Sets the category status to inactive.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductCategoryResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.deactivateCategory(id));
    }
}