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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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