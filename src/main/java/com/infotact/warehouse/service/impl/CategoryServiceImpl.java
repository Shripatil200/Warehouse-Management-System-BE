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

import java.util.List;
import java.util.stream.Collectors;


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

    //Get Single Category by id
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategory(String id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }


    // get all categories as list.
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getAllCategories() {
        List<ProductCategory> allCategories = categoryRepository.findAll();

        // Map everything to DTOs first
        return allCategories.stream()
                .filter(cat -> cat.getParentCategory() == null) // Start from Root categories
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        if (entity.getParentCategory() != null) {
            response.setParentCategoryName(entity.getParentCategory().getName());
        }

        // Recursively map children if they exist
        if (entity.getProducts() != null) { // Assuming you want sub-categories
            // Note: You'll need a List<ProductCategory> subCategories field in your Entity
            // if you want to navigate down easily, otherwise filter from 'allCategories'
        }

        return response;
    }


}
