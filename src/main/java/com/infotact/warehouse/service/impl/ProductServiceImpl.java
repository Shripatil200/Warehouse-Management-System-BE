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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        log.info("Creating a new product with SKU: {} ", request.getSku());

        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new AlreadyExistsException("Product with SKU " + request.getSku() + " already exist");
        }

        ProductCategory category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Active Category not found"));

        Product product = new Product();
        updateProductFields(product, request);
        product.setCategory(category);
        product.setActive(true);
        return mapToResponse(productRepository.save(product));
    }


    @Override
    public ProductResponse getProductById(String id) {

        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Override
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySkuAndActiveTrue(sku)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU: " + sku + "not found"));
    }


    @Override
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);

        // 1. Find the existing product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // 2. Check if SKU is being changed and if the new SKU already exists
        if (!product.getSku().equalsIgnoreCase(request.getSku()) &&
                productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new AlreadyExistsException("Product with SKU " + request.getSku() + " already exists");
        }

        // 3. Verify the new category exists and is active
        ProductCategory category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Active Category not found"));

        // 4. Update the fields using your helper
        updateProductFields(product, request);
        product.setCategory(category);

        // 5. Save and map to response
        return mapToResponse(productRepository.save(product));
    }


    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable, Boolean includeInactive) {
        log.info("getAllProducts include inactive: {}", includeInactive);
        Page<Product> product;

        if (includeInactive) {
            // Admin View: See everything for auditing
            product = productRepository.findAllByActiveTrue(pageable);
        } else {
            //  Operations View: Only active Products
            product = productRepository.findAll(pageable);
        }

        return product.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        log.info("Executing Soft Delete for product ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product Not found"));

        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void activateProduct(String id) {
        log.info("Executing Activate for product ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(true);
        productRepository.save(product);
    }


    // Helper to keep code DRY (Don't Repeat Yourself)
    private void updateProductFields(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setWeight(request.getWeight());
        product.setBarcode(request.getBarcode());
    }

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