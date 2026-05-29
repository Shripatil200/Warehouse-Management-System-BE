package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.ProductRepository;

import com.infotact.warehouse.service.ProductService;
import com.infotact.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository         productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserService               userService;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse addProduct(ProductRequest request) {
        User manager       = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        if (productRepository.existsBySkuIgnoreCaseAndWarehouseId(request.getSku(), warehouseId)) {
            throw new AlreadyExistsException(
                    "Product with SKU " + request.getSku() + " already exists in this facility.");
        }

        ProductCategory category = categoryRepository
                .findByIdAndWarehouseId(request.getCategoryId(), warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specified Category is invalid or belongs to another warehouse."));

        Product product = new Product();
        updateProductFields(product, request);
        product.setCategory(category);
        product.setWarehouse(manager.getWarehouse());
        product.setActive(true);

        log.info("Product '{}' (SKU: {}) registered for warehouse ID: {}",
                product.getName(), product.getSku(), warehouseId);

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        User manager = userService.getAuthenticatedUser();
        return productRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getProductBySku(String sku) {
        User manager = userService.getAuthenticatedUser();
        return productRepository.findBySkuAndWarehouseIdAndActiveTrue(sku, manager.getWarehouse().getId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active product with SKU '" + sku + "' not found."));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(String id, ProductRequest request) {
        User manager       = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        Product product = productRepository.findByIdAndWarehouseId(id, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSku().equalsIgnoreCase(request.getSku()) &&
                productRepository.existsBySkuIgnoreCaseAndWarehouseId(request.getSku(), warehouseId)) {
            throw new AlreadyExistsException(
                    "Collision detected: SKU " + request.getSku() + " is already assigned to another item.");
        }

        ProductCategory category = categoryRepository
                .findByIdAndWarehouseId(request.getCategoryId(), warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "New category assignment is invalid for this facility."));

        updateProductFields(product, request);
        product.setCategory(category);
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, String search, Boolean includeInactive) {
        User manager   = userService.getAuthenticatedUser();
        String whId    = manager.getWarehouse().getId();

        Page<Product> products;
        if (search != null && !search.isBlank()) {
            products = includeInactive
                    ? productRepository.searchProductsAll(whId, search, pageable)
                    : productRepository.searchProducts(whId, true, search, pageable);
        } else {
            products = includeInactive
                    ? productRepository.findAllByWarehouseId(whId, pageable)
                    : productRepository.findAllByWarehouseIdAndActiveTrue(whId, pageable);
        }

        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse deleteProduct(String id) {
        User manager = userService.getAuthenticatedUser();
        Product product = productRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse activateProduct(String id) {
        User manager = userService.getAuthenticatedUser();
        Product product = productRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(true);
        return mapToResponse(productRepository.save(product));
    }

    private void updateProductFields(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku().toUpperCase());
        product.setDescription(request.getDescription());
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());
        product.setUom(request.getUom());
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());
        product.setBarcode(request.getSku().toUpperCase());
        product.setMinThreshold(request.getMinThreshold()           != null ? request.getMinThreshold()           : 10);
        product.setMinReplenishThreshold(request.getMinReplenishThreshold() != null ? request.getMinReplenishThreshold() : 5);
        product.setMaxPickFaceCapacity(request.getMaxPickFaceCapacity()     != null ? request.getMaxPickFaceCapacity()   : 50);
        product.setMaxThreshold(request.getMaxThreshold());
        product.setSerialized(request.isSerialized());
        product.setBatchTracked(request.isBatchTracked());
    }

    private ProductResponse mapToResponse(Product entity) {
        long totalStock = entity.getInventoryItems() != null
                ? entity.getInventoryItems().stream().mapToLong(i -> (long) i.getQuantity()).sum()
                : 0L;

        boolean needsReplenishment = entity.getInventoryItems() != null &&
                entity.getInventoryItems().stream()
                        .filter(i -> i.getStorageBin().getBinType() == BinType.PICK_FACE)
                        .anyMatch(i -> i.getQuantity() < entity.getMinReplenishThreshold());

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
                .unitVolume(entity.getUnitVolume())
                .barcode(entity.getBarcode())
                .active(entity.isActive())
                .minThreshold(entity.getMinThreshold())
                .minReplenishThreshold(entity.getMinReplenishThreshold())
                .maxPickFaceCapacity(entity.getMaxPickFaceCapacity())
                .maxThreshold(entity.getMaxThreshold())
                .isLowStock(totalStock <= entity.getMinThreshold())
                .needsReplenishment(needsReplenishment)
                .isSerialized(entity.isSerialized())
                .isBatchTracked(entity.isBatchTracked())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        response.setSourcingOptions(List.of());

        return response;
    }
}
