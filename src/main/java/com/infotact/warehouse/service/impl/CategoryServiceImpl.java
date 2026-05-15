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
import com.infotact.warehouse.service.UserService;
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

import java.util.stream.Collectors;

/**
 * Implementation of {@link CategoryService} managing product taxonomy.
 * <p>
 * This service enforces multi-tenant isolation, ensuring that managers only
 * interact with categories belonging to their assigned warehouse. It supports
 * recursive nesting and provides safety checks for circular dependencies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;



    /**
     * {@inheritDoc}
     * <p>
     * <b>Validation:</b> Checks for name uniqueness within the specific warehouse context.
     * If a parent ID is provided, verifies it exists within the same facility.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse addCategory(ProductCategoryRequest request) {
        User manager = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        log.info("Attempting to create category '{}' for warehouse: {}", request.getName(), warehouseId);

        if (categoryRepository.existsByNameIgnoreCaseAndWarehouseId(request.getName(), warehouseId)) {
            throw new AlreadyExistsException("Category name '" + request.getName() + "' is already in use at this facility.");
        }

        ProductCategory category = new ProductCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setPreferredZoneId(request.getPreferredZoneId());
        category.setWarehouse(manager.getWarehouse());

        if (request.getParentCategoryId() != null) {
            ProductCategory parent = categoryRepository.findByIdAndWarehouseId(request.getParentCategoryId(), warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found or belongs to another warehouse."));
            category.setParentCategory(parent);
        }

        return mapToResponse(categoryRepository.save(category));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs a warehouse-scoped lookup.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public ProductCategoryResponse getCategory(String id) {
        User manager = userService.getAuthenticatedUser();
        return categoryRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a paginated list of categories mapped to the current user's facility.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductCategoryResponse> getAllCategories(Pageable pageable, boolean includeInactive) {
        User manager = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        Page<ProductCategory> categories = includeInactive ?
                categoryRepository.findAllByWarehouseId(warehouseId, pageable) :
                categoryRepository.findAllByWarehouseIdAndActiveTrue(warehouseId, pageable);

        return categories.map(this::mapToResponse);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Constraint:</b> Deletion is blocked if the category has active sub-categories
     * or associated product stock to maintain referential integrity.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(String id) {
        User manager = userService.getAuthenticatedUser();
        ProductCategory category = categoryRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found for deletion."));

        if (!category.getSubCategories().isEmpty()) {
            throw new IllegalOperationException("Cannot delete: Category has nested sub-categories. Delete children first.");
        }

        if (!category.getProducts().isEmpty()) {
            throw new IllegalOperationException("Cannot delete: Category is currently linked to active products.");
        }

        categoryRepository.delete(category);
        log.warn("Category '{}' (ID: {}) permanently removed from warehouse.", category.getName(), id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Logic:</b> Prevents circular references by ensuring a category cannot be
     * set as its own parent.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategoryResponse updateCategory(String id, ProductCategoryRequest request) {
        User manager = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        ProductCategory category = categoryRepository.findByIdAndWarehouseId(id, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found for update."));

        // Name Change Validation
        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByNameIgnoreCaseAndWarehouseId(request.getName(), warehouseId)) {
            throw new AlreadyExistsException("Another category in this warehouse is already named '" + request.getName() + "'.");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setPreferredZoneId(request.getPreferredZoneId());

        // Hierarchy Logic
        if (request.getParentCategoryId() != null) {
            if (id.equals(request.getParentCategoryId())) {
                throw new IllegalOperationException("A category cannot be assigned as its own parent.");
            }
            ProductCategory parent = categoryRepository.findByIdAndWarehouseId(request.getParentCategoryId(), warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category assignment invalid."));
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

    /**
     * Maps the JPA entity to a Response DTO including UI-centric metadata.
     */
    private ProductCategoryResponse mapToResponse(ProductCategory entity) {
        return ProductCategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .preferredZoneId(entity.getPreferredZoneId())
                .parentCategoryId(entity.getParentCategory() != null ? entity.getParentCategory().getId() : null)
                .parentCategoryName(entity.getParentCategory() != null ? entity.getParentCategory().getName() : null)
                // Metadata for frontend
                .isRoot(entity.getParentCategory() == null)
                .subCategoryCount(entity.getSubCategories() != null ? entity.getSubCategories().size() : 0)
                .productCount(entity.getProducts() != null ? entity.getProducts().size() : 0)
                // Recursive mapping for tree structures
                .children(entity.getSubCategories().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Shared logic for status toggles.
     */
    private ProductCategoryResponse updateStatus(String id, boolean status) {
        User manager = userService.getAuthenticatedUser();
        ProductCategory category = categoryRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        category.setActive(status);
        return mapToResponse(categoryRepository.save(category));
    }
}