package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Product Master Data management.
 * <p>
 * This service serves as the source of truth for the product catalog. It manages
 * product definitions, SKU integrity, and lifecycle states (Active/Inactive).
 * It ensures that all inventory operations reference valid, facility-authorized
 * product entities.
 * </p>
 */
public interface ProductService {

    /**
     * Registers a new product within the warehouse catalog.
     * <p>
     * <b>Integrity Rules:</b>
     * 1. <b>SKU Uniqueness:</b> Validates that the SKU is unique within the facility
     * using a case-insensitive check.
     * 2. <b>Category Binding:</b> Ensures the product is linked to an existing
     * and 'Active' {@link com.infotact.warehouse.entity.ProductCategory}.
     * </p>
     * @param request Product attributes (SKU, Name, Dimensions, Thresholds).
     * @return The persisted product mapped to a response DTO.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if SKU is taken.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if Category is invalid.
     */
    ProductResponse addProduct(ProductRequest request);

    /**
     * Retrieves product metadata using the internal system identifier.
     * @param id The UUID of the product.
     * @return Full product details.
     */
    ProductResponse getProductById(String id);

    /**
     * Resolves a product using its human-readable identifier (SKU).
     * <p>
     * <b>Operational Usage:</b> This is the primary entry point for
     * Barcode/RFID scanner integrations.
     * </p>
     * @param sku The business-facing unique code.
     * @return Product details if active.
     */
    ProductResponse getProductBySku(String sku);

    /**
     * Updates product specifications or classification.
     * <p>
     * <b>Note:</b> If the SKU is modified, the service re-validates
     * uniqueness against the global catalog to prevent collisions.
     * </p>
     */
    ProductResponse updateProduct(String id, ProductRequest request);

    /**
     * Provides a paginated view of the warehouse catalog.
     * <p>
     * <b>Filtering:</b> Defaults to 'Active' products for standard operations
     * (Picking/Receiving) but allows 'Inactive' inclusion for administrative audits.
     * </p>
     * @param pageable Pagination and sorting parameters.
     * @param includeInactive Toggle to include archived/soft-deleted items.
     */
    Page<ProductResponse> getAllProducts(Pageable pageable, Boolean includeInactive);

    /**
     * Archives a product via "Soft-Delete".
     * <p>
     * <b>Logic:</b> Sets 'active' to false. This prevents the product from
     * being ordered or received while preserving historical transaction
     * data for financial and audit reports.
     * </p>
     * @param id The UUID of the product.
     * @return The archived product details.
     */
    ProductResponse deleteProduct(String id);

    /**
     * Restores an archived product to the active catalog.
     */
    ProductResponse activateProduct(String id);
}