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
        return categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> getAllCategories(Pageable pageable) {
        log.debug("Fetching paginated categories");
        return categoryRepository.findAll(pageable)
                .map(this::mapToResponse);
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




    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        // Initialize as empty list instead of null to be Frontend-Friendly
        response.setChildren(new java.util.ArrayList<>());

        if (entity.getParentCategory() != null) {
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }
}