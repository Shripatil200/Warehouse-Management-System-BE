package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductCategoryRequest;
import com.infotact.warehouse.dto.v1.response.ProductCategoryResponse;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.IllegalOperationException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Implementation of {@link CategoryService} managing product taxonomy.
 * <p>
 * Supports hierarchical structures and Warehouse-specific storage optimization
 * through preferred zone mapping.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User profile not found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse addCategory(ProductCategoryRequest request) {
        log.info("Creating category '{}' for facility context.", request.getName());

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Category with this name already exists.");
        }

        User manager = getAuthenticatedUser();
        ProductCategory category = new ProductCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setPreferredZoneId(request.getPreferredZoneId());
        category.setWarehouse(manager.getWarehouse());

        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found."));
            category.setParentCategory(parent);
        }

        return mapToResponse(categoryRepository.save(category));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public ProductCategoryResponse getCategory(String id) {
        return categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'list-' + #includeInactive + '-' + #pageable.pageNumber")
    public Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        Page<ProductCategory> categories = includeInactive ?
                categoryRepository.findAllByWarehouseId(warehouseId, pageable) :
                categoryRepository.findAllByWarehouseIdAndActiveTrue(warehouseId, pageable);

        return categories.map(this::mapToResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(String id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getSubCategories().isEmpty() || !category.getProducts().isEmpty()) {
            throw new IllegalOperationException("Cannot delete category: It contains linked products or sub-categories.");
        }

        categoryRepository.delete(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setPreferredZoneId(request.getPreferredZoneId());

        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found."));
            category.setParentCategory(parent);
        }

        return mapToResponse(categoryRepository.save(category));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse activateCategory(String id) {
        return updateStatus(id, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse deactivateCategory(String id) {
        return updateStatus(id, false);
    }

    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        ProductCategoryResponse response = new ProductCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setPreferredZoneId(entity.getPreferredZoneId());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setChildren(new ArrayList<>()); // Placeholder for tree mapping

        if (entity.getParentCategory() != null) {
            response.setParentCategoryId(entity.getParentCategory().getId());
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    private ProductCategoryResponse updateStatus(String id, boolean status) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(status);
        return mapToResponse(categoryRepository.save(category));
    }
}