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


@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;

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

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategory(String id) {
        // Find by ID regardless of status; logic below handles access control
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive) {
        log.debug("Fetching categories. Include Inactive: {}", includeInactive);

        Page<ProductCategory> categories;
        if (includeInactive) {
            // Admin View: See everything for auditing [cite: 139-145]
            categories = categoryRepository.findAll(pageable);
        } else {
            // Operations View: Only active categories [cite: 161]
            categories = categoryRepository.findAllByActiveTrue(pageable);
        }

        return categories.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteCategory(String id) {
        log.info("Deleting category with id: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Industry Best Practice: Check for children and products separately for specific error messages
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new IllegalOperationException("Please delete all sub-categories before deleting this category.");
        }

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalOperationException("Cannot delete category as it contains active products.");
        }

        categoryRepository.delete(category);
    }


    @Override
    @Transactional
    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request) {
        log.info("Updating category with id: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // 1. If name is changing, check for duplicates
        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        // 2. Prevent self-parenting
        if (id.equals(request.getParentCategoryId())) {
            throw new IllegalOperationException("A category cannot be its own parent.");
        }

        category.setName(request.getName());

        // 3. Handle Parent update
        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        return mapToResponse(categoryRepository.save(category));
    }




    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setActive(entity.isActive());

        // These will now resolve correctly
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        response.setChildren(new java.util.ArrayList<>());

        if (entity.getParentCategory() != null) {
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }


    @Override
    @Transactional
    public ProductCategoryResponse activateCategory(String id) {
        log.info("Activating category: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setActive(true);

        // 1. Save and capture the updated entity
        ProductCategory updatedCategory = categoryRepository.save(category);

        // 2. Map the entity to your Response DTO and return it
        // Replace 'categoryMapper' with whatever name you've given your mapper bean
        return mapToResponse(updatedCategory);
    }


    @Override
    @Transactional
    public ProductCategoryResponse deactivateCategory(String id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setActive(false);
        // The save method returns the updated entity
        ProductCategory updatedCategory = categoryRepository.save(category);

        // Convert entity to Response DTO and return it
        return mapToResponse(updatedCategory);
    }
}