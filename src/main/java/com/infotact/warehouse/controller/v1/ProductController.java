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
 * REST controller for managing the warehouse product catalog.
 * Access is strictly restricted to users with the MANAGER role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')") // Global security gate for all product operations
@Tag(name = "Product Management", description = "Endpoints for managing the warehouse inventory catalog")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Register a new product", description = "Adds a product to the catalog. Requires a unique SKU and an active category ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data or inactive category"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Access restricted to Managers"),
            @ApiResponse(responseCode = "409", description = "SKU already exists in the system")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request){
        log.info("REST request to create new Product with SKU: {}", request.getSku());
        return new ResponseEntity<>(productService.addProduct(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get product by ID", description = "Retrieves a product's full details using its internal UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "The internal UUID of the product") @PathVariable String id){
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Get product by SKU", description = "Retrieves a product using its business SKU. Useful for barcode scanner integrations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "No active product found with this SKU")
    })
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "The unique Stock Keeping Unit (SKU)") @PathVariable String sku){
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @Operation(summary = "Update product", description = "Updates pricing, descriptions, or category links. SKU changes are validated for uniqueness.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        log.info("REST request to update product with id: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(summary = "List all products", description = "Returns a paginated list of inventory. Defaults to showing only active products.")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @ParameterObject Pageable pageable,
            @Parameter(description = "If true, includes soft-deleted/inactive items")
            @RequestParam(defaultValue = "false") boolean includeInactive){
        return ResponseEntity.ok(productService.getAllProducts(pageable, includeInactive));
    }

    @Operation(summary = "Delete (Deactivate) a product", description = "Performs a soft delete by setting the product to inactive. Product remains in history for orders.")
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<ProductResponse> deleteProduct(@PathVariable String id){
        log.warn("REST request to deactivate product: {}", id);
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @Operation(summary = "Activate a product", description = "Restores an inactive/soft-deleted product to active status.")
    @PatchMapping(path="/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable String id){
        return ResponseEntity.ok(productService.activateProduct(id));
    }
}