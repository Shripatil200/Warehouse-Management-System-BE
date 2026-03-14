package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;

    @Transactional
    public ProductCategoryResponse addCategory(ProductCategoryRequest request) {
        ProductCategory category = new ProductCategory();
        category.setName(request.getName());

        // Handle Parent Category association
        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParentCategory(parent);
        }

        ProductCategory savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategory(String id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    // Manual mapping (Cleaner and faster than reflection-based mappers for simple cases)
    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        if (entity.getParentCategory() != null) {
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }


}
