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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Implementation of {@link CategoryService}.
 * Focuses on maintaining data integrity within the warehouse category tree.
 * Secured to ensure only Managers can execute business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')") // Redundant but safe secondary security layer
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductCategoryResponse addCategory(ProductCategoryRequest request) {
        log.info("Adding new category: {}", request.getName());

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        ProductCategory category = new ProductCategory();
        category.setName(request.getName());

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
            categories = categoryRepository.findAll(pageable);
        } else {
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

        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new IllegalOperationException("Please delete all sub-categories before deleting this category.");
        }

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

        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        if (id.equals(request.getParentCategoryId())) {
            throw new IllegalOperationException("A category cannot be its own parent.");
        }

        category.setName(request.getName());

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
     */
    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setChildren(new ArrayList<>());

        if (entity.getParentCategory() != null) {
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }

    /**
     * Common logic for status toggling.
     */
    private ProductCategoryResponse updateStatus(String id, boolean status) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setActive(status);
        return mapToResponse(categoryRepository.save(category));
    }
}