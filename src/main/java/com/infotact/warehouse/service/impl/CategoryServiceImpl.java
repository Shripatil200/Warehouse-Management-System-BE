package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.entity.ProductCategory;
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