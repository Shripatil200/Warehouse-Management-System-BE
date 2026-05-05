package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.dto.v1.response.ProductSupplierResponse;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.ProductSupplierRepository;
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

import java.util.stream.Collectors;

/**
 * Implementation of {@link ProductService} for core catalog orchestration.
 * <p>
 * This service manages the lifecycle of warehouse inventory items. It enforces
 * strict multi-tenant isolation by scoping all operations to the authenticated
 * manager's warehouse.
 * </p>
 * <p>
 * <b>Update Highlights (v3.2):</b>
 * <ul>
 *     <li><b>Replenishment Logic:</b> Captures {@code minReplenishThreshold} and {@code maxPickFaceCapacity} for automated Bulk-to-Picking moves.</li>
 *     <li><b>Picking Health:</b> Dynamic response mapping now identifies if a product requires internal movement (needsReplenishment).</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final UserService userService;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Validation:</b> Enforces SKU uniqueness within the specific warehouse context.
     * Verifies that the assigned category belongs to the same facility.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse addProduct(ProductRequest request) {
        User manager = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        if (productRepository.existsBySkuIgnoreCaseAndWarehouseId(request.getSku(), warehouseId)) {
            throw new AlreadyExistsException("Product with SKU " + request.getSku() + " already exists in this facility.");
        }

        ProductCategory category = categoryRepository.findByIdAndWarehouseId(request.getCategoryId(), warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Specified Category is invalid or belongs to another warehouse."));

        Product product = new Product();
        updateProductFields(product, request);
        product.setCategory(category);
        product.setWarehouse(manager.getWarehouse());
        product.setActive(true);

        log.info("Catalog Update: Product '{}' (SKU: {}) registered for warehouse ID: {}",
                product.getName(), product.getSku(), warehouseId);

        return mapToResponse(productRepository.save(product));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs a warehouse-scoped lookup to prevent cross-tenant data leakage.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        User manager = userService.getAuthenticatedUser();
        return productRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product record not found for ID: " + id));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Optimized for high-speed scanning and barcode lookups.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getProductBySku(String sku) {
        User manager = userService.getAuthenticatedUser();
        return productRepository.findBySkuAndWarehouseIdAndActiveTrue(sku, manager.getWarehouse().getId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Active product with SKU '" + sku + "' not found."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Updates product metadata. Re-validates SKU uniqueness if the SKU is modified.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(String id, ProductRequest request) {
        User manager = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        Product product = productRepository.findByIdAndWarehouseId(id, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSku().equalsIgnoreCase(request.getSku()) &&
                productRepository.existsBySkuIgnoreCaseAndWarehouseId(request.getSku(), warehouseId)) {
            throw new AlreadyExistsException("Collision detected: SKU " + request.getSku() + " is already assigned to another item.");
        }

        ProductCategory category = categoryRepository.findByIdAndWarehouseId(request.getCategoryId(), warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("New category assignment is invalid for this facility."));

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
        User manager = userService.getAuthenticatedUser();
        String whId = manager.getWarehouse().getId();

        Page<Product> products = includeInactive ?
                productRepository.findAllByWarehouseId(whId, pageable) :
                productRepository.findAllByWarehouseIdAndActiveTrue(whId, pageable);

        return products.map(this::mapToResponse);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets 'active' flag to false to preserve transaction history while disabling catalog access.
     * </p>
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * Synchronizes DTO fields to the JPA entity.
     * Includes inheritance of industrial replenishment thresholds.
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

        // Replenishment & Safety Controls
        product.setMinThreshold(request.getMinThreshold() != null ? request.getMinThreshold() : 10);
        product.setMinReplenishThreshold(request.getMinReplenishThreshold() != null ? request.getMinReplenishThreshold() : 5);
        product.setMaxPickFaceCapacity(request.getMaxPickFaceCapacity() != null ? request.getMaxPickFaceCapacity() : 50);
        product.setMaxThreshold(request.getMaxThreshold());

        // Traceability
        product.setSerialized(request.isSerialized());
        product.setBatchTracked(request.isBatchTracked());
    }

    /**
     * Maps Entity to Response DTO including dynamic health indicators for Bulk and Picking.
     */
    private ProductResponse mapToResponse(Product entity) {
        // Calculate Total Warehouse Stock (Bulk + Picking) for reorder alerts
        long totalStock = entity.getInventoryItems() != null ?
                entity.getInventoryItems().stream().mapToLong(i -> (long) i.getQuantity()).sum() : 0L;

        // Check if any PICK_FACE bin specifically is low (needs replenishment move)
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
                .needsReplenishment(needsReplenishment) // Added indicator
                .isSerialized(entity.isSerialized())
                .isBatchTracked(entity.isBatchTracked())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Vendor sourcing mapping
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