package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.dto.v1.response.ProductSupplierResponse;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.ProductSupplierRepository;
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

import java.util.stream.Collectors;

/**
 * Implementation of {@link ProductService} for core catalog orchestration.
 * <p>
 * This service manages the lifecycle of warehouse inventory items. It enforces
 * data integrity via SKU uniqueness checks and bridges human-readable master
 * data with physical inventory tracking.
 * </p>
 * <p>
 * <b>Update:</b> Supports advanced logistics (Dimensions, UOM) and
 * traceability flags (Serialized/Batch) for warehouse optimization.
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
    private final ProductSupplierRepository productSupplierRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }
    /**
     * {@inheritDoc}
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
        product.setWarehouse(manager.getWarehouse());
        product.setActive(true);

        log.info("Catalog Update: Product '{}' (SKU: {}) registered for facility.", product.getName(), product.getSku());
        return mapToResponse(productRepository.save(product));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product record not found for ID: " + id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySkuAndActiveTrue(sku)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Active product with SKU '" + sku + "' not found."));
    }

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

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        return mapToResponse(productRepository.save(product));
    }

    /**
     * {@inheritDoc}
     */
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
     * <p>
     * <b>Update:</b> Now handles financial valuation (costPrice) and
     * volumetric data (L/W/H) required for bin-capacity calculations.
     * </p>
     */
    private void updateProductFields(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku().toUpperCase());
        product.setDescription(request.getDescription());

        // Financials
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());

        // Logistics
        product.setUom(request.getUom());
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());

        product.setBarcode(request.getBarcode());

        // Operational Logic
        product.setMinThreshold(request.getMinThreshold() != null ? request.getMinThreshold() : 10);
        product.setMaxThreshold(request.getMaxThreshold());

        // Traceability
        product.setSerialized(request.isSerialized());
        product.setBatchTracked(request.isBatchTracked());
    }

    private ProductResponse mapToResponse(Product entity) {
        ProductResponse response = ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sku(entity.getSku())
                .description(entity.getDescription())
                .sellingPrice(entity.getSellingPrice())
                .costPrice(entity.getCostPrice())
                .uom(entity.getUom())
                .weight(entity.getWeight())
                .length(entity.getLength())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .barcode(entity.getBarcode())
                .active(entity.isActive())
                .minThreshold(entity.getMinThreshold())
                .maxThreshold(entity.getMaxThreshold())
                .isSerialized(entity.isSerialized())
                .isBatchTracked(entity.isBatchTracked())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        response.setSourcingOptions(
                productSupplierRepository.findByProductId(entity.getId()).stream()
                        .map(ps -> ProductSupplierResponse.builder()
                                .supplierName(ps.getSupplier().getName())
                                .currentSupplyPrice(ps.getCurrentSupplyPrice())
                                .leadTimeDays(ps.getLeadTimeDays())
                                .build())
                        .collect(Collectors.toList())
        );

        return response;
    }
}