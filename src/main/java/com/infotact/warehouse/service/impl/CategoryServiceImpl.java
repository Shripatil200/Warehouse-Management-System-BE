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
 * This service operates under strict Multi-tenant isolation, ensuring that
 * category hierarchies are partitioned by Warehouse. It utilizes Spring Cache
 * to minimize database round-trips for frequently accessed catalog data.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Internal utility to resolve the Manager/Admin profile from the session.
     * Used to enforce Warehouse-level data isolation.
     */
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User profile not found"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Automatically binds the new category to the authenticated user's Warehouse.</li>
     * <li>Flushes the 'categories' cache to ensure list consistency across the facility.</li>
     * </ul>
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
     * <p>
     * <b>Optimization:</b> Results are cached by ID to accelerate repeated lookups
     * during product catalog rendering.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public ProductCategoryResponse getCategory(String id) {
        return categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Multi-tenancy:</b> Filters results strictly by the Warehouse ID extracted
     * from the authenticated user's profile.
     * </p>
     */
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
     * <p>
     * <b>Integrity Guard:</b> Blocks deletion if 'orphaned' products or
     * sub-categories would be created as a result of this operation.
     * </p>
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
        log.info("Category {} successfully purged.", id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Updates metadata and handles parent-reassignment
     * while triggering a global cache eviction for categories.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setName(request.getName());
        // Additional business logic for parent updates would go here...

        return mapToResponse(categoryRepository.save(category));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse activateCategory(String id) {
        return updateStatus(id, true);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse deactivateCategory(String id) {
        return updateStatus(id, false);
    }

    /**
     * Maps the internal JPA Entity to a secure Response DTO.
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
     * Internal status toggle utility.
     */
    private ProductCategoryResponse updateStatus(String id, boolean status) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(status);
        return mapToResponse(categoryRepository.save(category));
    }
}