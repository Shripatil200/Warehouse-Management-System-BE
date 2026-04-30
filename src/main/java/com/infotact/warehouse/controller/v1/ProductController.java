package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductRequest;
import com.infotact.warehouse.dto.v1.response.ProductResponse;
import com.infotact.warehouse.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * This controller provides endpoints for the creation, modification, and lifecycle management
 * of products. All operations are strictly isolated to the warehouse context associated
 * with the authenticated MANAGER.
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
     * Registers a new product in the facility's catalog.
     *
     * @param request The product details including SKU, price, and category mapping.
     * @return The created product details.
     */
    @Operation(
            summary = "Register a new product",
            description = "Adds a product to the catalog. SKU must be unique within the current warehouse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data or inactive category"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only Managers can modify the catalog"),
            @ApiResponse(responseCode = "409", description = "Conflict: Product SKU already exists in this warehouse")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request){
        log.info("REST request to register new Product with SKU: {}", request.getSku());
        return new ResponseEntity<>(productService.addProduct(request), HttpStatus.CREATED);
    }

    /**
     * Fetches a product using its internal database UUID.
     *
     * @param id The internal UUID of the product.
     * @return Detailed product response.
     */
    @Operation(
            summary = "Get product by ID",
            description = "Retrieves full specifications of a product using its unique UUID. Scoped to current warehouse."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "The internal UUID of the product", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id){
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Fetches a product using its Stock Keeping Unit (SKU).
     *
     * @param sku The unique business-level SKU.
     * @return Product details if found.
     */
    @Operation(
            summary = "Get product by SKU",
            description = "High-performance lookup by SKU. Ideal for scanner integrations. Scoped to current warehouse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "No active product found with this SKU in current warehouse")
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
    @Operation(
            summary = "Update product",
            description = "Updates pricing, descriptions, or category. SKU uniqueness is enforced within the warehouse."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        log.info("REST request to update product: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Retrieves a paginated list of all products for the authenticated manager's facility.
     *
     * @param pageable Pagination and sorting criteria.
     * @param includeInactive Boolean flag to include deactivated (soft-deleted) products.
     */
    @Operation(
            summary = "List all products",
            description = "Returns a paginated view of the catalog for the current warehouse."
    )
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
     * Logic: The product is marked inactive and hidden from standard lookups but preserved for
     * historical referential integrity.
     * </p>
     */
    @Operation(
            summary = "Deactivate product",
            description = "Performs a soft delete by marking the product as inactive."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductResponse> deleteProduct(@PathVariable String id){
        log.warn("REST request to deactivate product: {}", id);
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    /**
     * Restores an inactive product to the catalog.
     */
    @Operation(
            summary = "Activate product",
            description = "Restores a previously deactivated product to the active catalog."
    )
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable String id){
        return ResponseEntity.ok(productService.activateProduct(id));
    }
}