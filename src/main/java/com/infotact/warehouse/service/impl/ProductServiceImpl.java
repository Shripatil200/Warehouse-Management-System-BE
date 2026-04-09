package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.ProductService;
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

/**
 * Implementation of {@link ProductService} for core catalog orchestration.
 * <p>
 * This service manages the lifecycle of warehouse inventory items. It enforces
 * data integrity via SKU uniqueness checks and bridges human-readable master
 * data with physical inventory tracking.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Resolves the identity of the current user to enforce warehouse-level
     * multi-tenancy during product creation.
     */
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li><b>SKU Guard:</b> Performs a case-insensitive check to prevent duplicate identification.</li>
     * <li><b>Category Linkage:</b> Ensures the product is bound only to an 'Active' taxonomy node.</li>
     * <li><b>Cache Policy:</b> Triggers a global eviction of the 'products' cache to maintain list integrity.</li>
     * </ul>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse addProduct(ProductRequest request) {
        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new AlreadyExistsException("Product with SKU " + request.getSku() + " already exists");
        }

        User manager = getAuthenticatedUser();
        ProductCategory category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Specified Category is either inactive or does not exist."));

        Product product = new Product();
        updateProductFields(product, request);
        product.setCategory(category);
        product.setWarehouse(manager.getWarehouse()); // DENORMALIZATION: Optimized for facility-scoped lookups
        product.setActive(true);

        log.info("Catalog Update: Product '{}' (SKU: {}) registered for facility.", product.getName(), product.getSku());
        return mapToResponse(productRepository.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product record not found for ID: " + id));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Optimization:</b> Results are cached by SKU, significantly reducing latency
     * for barcode scanning workflows in the physical warehouse.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySkuAndActiveTrue(sku)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Active product with SKU '" + sku + "' not found."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Validation:</b> If the SKU is updated, the service re-verifies uniqueness
     * across the global catalog before committing the change.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSku().equalsIgnoreCase(request.getSku()) &&
                productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new AlreadyExistsException("Collision detected: SKU " + request.getSku() + " is assigned to another product.");
        }

        ProductCategory category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("New category assignment is invalid or inactive."));

        updateProductFields(product, request);
        product.setCategory(category);

        return mapToResponse(productRepository.save(product));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Pagination:</b> Cached by page index and status filter to accelerate
     * UI table rendering for staff members.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #includeInactive")
    public Page<ProductResponse> getAllProducts(Pageable pageable, Boolean includeInactive) {
        Page<Product> products = includeInactive ?
                productRepository.findAll(pageable) :
                productRepository.findAllByActiveTrue(pageable);
        return products.map(this::mapToResponse);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Soft-Delete Policy:</b> Deactivates the product to hide it from
     * operational views while preserving historical order and inventory records.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        log.warn("Catalog Management: Product {} has been deactivated (Soft-Deleted).", product.getSku());
        return mapToResponse(productRepository.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse activateProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(true);
        return mapToResponse(productRepository.save(product));
    }

    /**
     * Synchronizes DTO fields to the JPA entity.
     */
    private void updateProductFields(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku().toUpperCase()); // Logic: Standardize SKU case
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setWeight(request.getWeight());
        product.setBarcode(request.getBarcode());
        product.setMinThreshold(request.getMinThreshold() != null ? request.getMinThreshold() : 10);
    }

    /**
     * Maps the internal Product entity to a builder-pattern based Response DTO.
     */
    private ProductResponse mapToResponse(Product entity) {
        return ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sku(entity.getSku())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .weight(entity.getWeight())
                .barcode(entity.getBarcode())
                .active(entity.isActive())
                .minThreshold(entity.getMinThreshold())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}