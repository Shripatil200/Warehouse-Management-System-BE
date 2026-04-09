package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the master product catalog.
 * <p>
 * This controller allows for the creation, modification, and lifecycle management
 * of products. It supports soft-deletion (deactivation) to maintain referential
 * integrity with historical orders. Access is strictly controlled via the
 * <b>MANAGER</b> role.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "3. Product Management", description = "Endpoints for managing the master warehouse product catalog")
public class ProductController {

    private final ProductService productService;

    /**
     * Registers a new product in the system.
     * <p>
     * Logic: Validates that the SKU is unique across the system and that the
     * associated category exists and is active.
     * </p>
     *
     * @param request The product details including SKU, price, and category mapping.
     * @return The created product details.
     */
    @Operation(summary = "Register a new product",
            description = "Adds a product to the catalog. Requires a unique SKU and an active category reference.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data or inactive category"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only Managers can modify the catalog"),
            @ApiResponse(responseCode = "409", description = "Conflict: Product SKU already exists")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request){
        log.info("REST request to create new Product with SKU: {}", request.getSku());
        return new ResponseEntity<>(productService.addProduct(request), HttpStatus.CREATED);
    }

    /**
     * Fetches a product using its internal database UUID.
     *
     * @param id The internal UUID of the product.
     * @return Detailed product response.
     */
    @Operation(summary = "Get product by ID", description = "Retrieves full product specifications using its unique UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "The internal UUID of the product", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id){
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Fetches a product using its Stock Keeping Unit (SKU).
     * <p>
     * Optimization: This endpoint is indexed and specifically designed for fast
     * response times during barcode scanning or manual search by staff.
     * </p>
     *
     * @param sku The unique business-level SKU.
     * @return Product details if found.
     */
    @Operation(summary = "Get product by SKU", description = "High-performance lookup by SKU. Ideal for scanner integrations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "No active product found with this SKU")
    })
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "The unique Stock Keeping Unit (SKU)", example = "AUDIO-SNY-4000")
            @PathVariable String sku){
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    /**
     * Updates an existing product's metadata or category links.
     */
    @Operation(summary = "Update product", description = "Updates pricing, descriptions, or category. SKU uniqueness is enforced on change.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        log.info("REST request to update product with id: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Retrieves a paginated list of all products.
     *
     * @param pageable Pagination and sorting criteria.
     * @param includeInactive Boolean flag to include deactivated (soft-deleted) products.
     */
    @Operation(summary = "List all products", description = "Returns a paginated view of the catalog. Filters can be applied for active status.")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @ParameterObject Pageable pageable,
            @Parameter(description = "If true, includes deactivated products in the result")
            @RequestParam(defaultValue = "false") boolean includeInactive){
        return ResponseEntity.ok(productService.getAllProducts(pageable, includeInactive));
    }

    /**
     * Deactivates a product (Soft Delete).
     * <p>
     * Logic: The product is not removed from the database to ensure that historical
     * orders still have a valid reference. It is simply hidden from standard lookups.
     * </p>
     */
    @Operation(summary = "Deactivate product", description = "Performs a soft delete by marking the product as inactive.")
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<ProductResponse> deleteProduct(@PathVariable String id){
        log.warn("REST request to deactivate product: {}", id);
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @Operation(summary = "Activate product", description = "Restores a previously deactivated product to the active catalog.")
    @PatchMapping(path="/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable String id){
        return ResponseEntity.ok(productService.activateProduct(id));
    }
}