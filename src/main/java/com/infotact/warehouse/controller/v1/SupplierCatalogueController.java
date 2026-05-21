package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.SupplierProductRequest;
import com.infotact.warehouse.dto.v1.response.SupplierProductResponse;
import com.infotact.warehouse.service.SupplierCatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for supplier product catalogue management.
 * <p>
 * Suppliers manage their own offerings (price, lead time) here.
 * Warehouse managers view all supplier offers for a product to compare before raising a PO.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/supplier-catalogue")
@RequiredArgsConstructor
@Tag(name = "7. Supplier Product Catalogue", description = "Supplier product offerings — manage and compare prices and lead times")
public class SupplierCatalogueController {

    private final SupplierCatalogueService supplierCatalogueService;

    @Operation(
            summary = "Add a product to my catalogue",
            description = "Authenticated supplier lists a new product offering with price and lead time."
    )
    @PostMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierProductResponse> addProduct(@Valid @RequestBody SupplierProductRequest request) {
        return new ResponseEntity<>(supplierCatalogueService.addProduct(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update my product offering",
            description = "Updates the supply price or lead time for one of the authenticated supplier's listings."
    )
    @PutMapping("/{supplierProductId}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierProductResponse> updateProduct(
            @PathVariable String supplierProductId,
            @Valid @RequestBody SupplierProductRequest request) {
        return ResponseEntity.ok(supplierCatalogueService.updateProduct(supplierProductId, request));
    }

    @Operation(
            summary = "Deactivate a product listing",
            description = "Soft-deactivates a supplier product. Historical PO data is preserved."
    )
    @DeleteMapping("/{supplierProductId}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<Void> deactivateProduct(@PathVariable String supplierProductId) {
        supplierCatalogueService.deactivateProduct(supplierProductId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get my product catalogue",
            description = "Returns all active product offerings for the authenticated supplier."
    )
    @GetMapping("/mine")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<List<SupplierProductResponse>> getMyCatalogue() {
        return ResponseEntity.ok(supplierCatalogueService.getMyCatalogue());
    }

    @Operation(
            summary = "Compare all supplier offers for a product",
            description = "Returns all active offers for a given ProductMaster, ordered by price ascending. " +
                    "Used by warehouse managers when selecting a supplier for a Purchase Order."
    )
    @GetMapping("/product-master/{productMasterId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupplierProductResponse>> getOffersForProduct(@PathVariable String productMasterId) {
        return ResponseEntity.ok(supplierCatalogueService.getOffersForProduct(productMasterId));
    }
}
