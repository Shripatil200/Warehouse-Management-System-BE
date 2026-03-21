package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.IllegalOperationException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CategoryService}.
 * Focuses on maintaining data integrity within the warehouse category tree.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductCategoryResponse addCategory(ProductCategoryRequest request) {
        log.info("Adding new category: {}", request.getName());

        // Business Rule: Category names must be unique regardless of letter casing
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        ProductCategory category = new ProductCategory();
        category.setName(request.getName());

        // Handle hierarchical linking if a parent ID is provided
        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        }

        return mapToResponse(categoryRepository.save(category));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategory(String id) {
        // Fetching by ID; visibility logic is typically handled at the Controller/Security layer
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        return mapToResponse(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive) {
        log.debug("Fetching categories. Include Inactive: {}", includeInactive);

        Page<ProductCategory> categories;
        if (includeInactive) {
            // Admin/Audit View: Retrieve all records including disabled ones
            categories = categoryRepository.findAll(pageable);
        } else {
            // Standard Operational View: Filter out deactivated categories
            categories = categoryRepository.findAllByActiveTrue(pageable);
        }

        return categories.map(this::mapToResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteCategory(String id) {
        log.info("Deleting category with id: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Referential Integrity: Prevent deleting categories that act as parents to other categories
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new IllegalOperationException("Please delete all sub-categories before deleting this category.");
        }

        // Referential Integrity: Prevent orphaned products by blocking deletion of populated categories
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalOperationException("Cannot delete category as it contains active products.");
        }

        categoryRepository.delete(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request) {
        log.info("Updating category with id: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Only check name uniqueness if the name is actually being modified
        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        // Prevent Tree Inconsistency: A node cannot be its own parent
        if (id.equals(request.getParentCategoryId())) {
            throw new IllegalOperationException("A category cannot be its own parent.");
        }

        category.setName(request.getName());

        // Update parent reference; null indicates a top-level root category
        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        return mapToResponse(categoryRepository.save(category));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductCategoryResponse activateCategory(String id) {
        log.info("Activating category: {}", id);
        return updateStatus(id, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductCategoryResponse deactivateCategory(String id) {
        log.info("Deactivating category: {}", id);
        return updateStatus(id, false);
    }

    /**
     * Internal utility to map Entity state to a Response DTO.
     * Includes basic relationship resolution for the parent name.
     */
    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Initialize empty list to avoid null pointers in UI/Frontend mapping
        response.setChildren(new java.util.ArrayList<>());

        if (entity.getParentCategory() != null) {
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }

    /**
     * Common logic for status toggling to avoid code duplication.
     */
    private ProductCategoryResponse updateStatus(String id, boolean status) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setActive(status);
        return mapToResponse(categoryRepository.save(category));
    }
}