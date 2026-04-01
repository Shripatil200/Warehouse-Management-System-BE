package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the product lifecycle.
 * Handles inventory cataloging, SKU uniqueness, and visibility states.
 */
public interface ProductService {

    /**
     * Registers a new product in the warehouse.
     * @param request The product details including SKU, price, and category.
     * @return The created product response.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if the SKU is already in use.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the associated category is invalid or inactive.
     */
    ProductResponse addProduct(ProductRequest request);

    /**
     * Retrieves a product by its internal database ID.
     * @param id The UUID of the product.
     * @return The found product details.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if no product exists with that ID.
     */
    ProductResponse getProductById(String id);

    /**
     * Retrieves a product using its unique Stock Keeping Unit (SKU).
     * @param sku The business-facing unique code for the product.
     * @return The found product details.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if no active product exists with that SKU.
     */
    ProductResponse getProductBySku(String sku);

    /**
     * Updates an existing product's information.
     * @param id The UUID of the product to update.
     * @param request The updated data.
     * @return The updated product details.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if the new SKU conflicts with another product.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the product or new category is not found.
     */
    ProductResponse updateProduct(String id, ProductRequest request);

    /**
     * Retrieves a paginated list of products.
     * @param pageable Pagination and sorting parameters.
     * @param includeInactive If true, returns all products; if false, filters for active inventory only.
     * @return A page of product responses.
     */
    Page<ProductResponse> getAllProducts(Pageable pageable, Boolean includeInactive);

    /**
     * Deactivates a product (Soft Delete).
     * The product remains in the database for historical records but is hidden from operational views.
     * @param id The UUID of the product to deactivate.
     * @return The updated product with active set to false.
     */
    ProductResponse deleteProduct(String id);

    /**
     * Reactivates a previously soft-deleted product.
     * @param id The UUID of the product.
     * @return The updated product with active set to true.
     */
    ProductResponse activateProduct(String id);
}