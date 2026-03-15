package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ProductCategoryResponse> addCategory(@Valid @RequestBody ProductCategoryRequest request) {
        return new ResponseEntity<>(categoryService.addCategory(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> getCategory(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductCategoryResponse>> getAllCategories(
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        log.info("REST request to get all categories. includeInactive={}", includeInactive);
        return ResponseEntity.ok(categoryService.getAllCategories(pageable, includeInactive));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody ProductCategoryRequest request) {

        log.info("REST request to update category: {}", id);
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }


    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductCategoryResponse> activate(@PathVariable String id) {
        // Capture the response from the service
        ProductCategoryResponse response = categoryService.activateCategory(id);

        // Return 200 OK with the body instead of 204 No Content
        return ResponseEntity.ok(response);
    }



    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductCategoryResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.deactivateCategory(id));
    }
}