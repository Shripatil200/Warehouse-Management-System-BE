package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link ProductService}.
 * Manages the core inventory of the warehouse, ensuring SKU uniqueness
 * and validating category relationships.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        log.info("Creating a new product with SKU: {} ", request.getSku());

        // Guard Clause: Prevent duplicate SKUs regardless of casing
        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new AlreadyExistsException("Product with SKU " + request.getSku() + " already exist");
        }

        // Integrity Check: Products can only be assigned to categories that are currently 'Active'
        ProductCategory category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Active Category not found"));

        Product product = new Product();
        updateProductFields(product, request);
        product.setCategory(category);
        product.setActive(true); // New products are active by default

        return mapToResponse(productRepository.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        // Only active products are returned via SKU lookup to avoid operational errors with deleted stock
        return productRepository.findBySkuAndActiveTrue(sku)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU: " + sku + " not found"));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Validation: If SKU is changing, ensure the new one isn't already taken
        if (!product.getSku().equalsIgnoreCase(request.getSku()) &&
                productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new AlreadyExistsException("Product with SKU " + request.getSku() + " already exists");
        }

        // Validation: Ensure the target category exists and is enabled
        ProductCategory category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Active Category not found"));

        updateProductFields(product, request);
        product.setCategory(category);

        return mapToResponse(productRepository.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, Boolean includeInactive) {
        Page<Product> products;

        if (includeInactive) {
            // Administrative View: Useful for stock recovery or history auditing
            products = productRepository.findAll(pageable);
        } else {
            // Operations View: Standard view to prevent sales/picks of deactivated items
            products = productRepository.findAllByActiveTrue(pageable);
        }

        return products.map(this::mapToResponse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductResponse deleteProduct(String id) {
        log.info("Executing Soft Delete for product ID: {}", id);

        // Soft Delete: We set 'active' to false instead of removing the record
        // to maintain referential integrity with historical Order data.
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Not found"));

        product.setActive(false);
        return mapToResponse(productRepository.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProductResponse activateProduct(String id) {
        log.info("Executing Activate for product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(true);
        return mapToResponse(productRepository.save(product));
    }

    /**
     * Helper method to map basic fields from a Request DTO to a Product Entity.
     * Centralizing this logic keeps the service methods focused on business rules (DRY).
     */
    private void updateProductFields(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setWeight(request.getWeight());
        product.setBarcode(request.getBarcode());
    }

    /**
     * Converts a Product entity into a Response DTO.
     * Maps relationship fields (Category) into flat DTO strings for easier API consumption.
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
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}