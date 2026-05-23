package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ProductMasterRequest;
import com.infotact.warehouse.dto.v1.response.ProductMasterResponse;
import com.infotact.warehouse.service.ProductMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the global ProductMaster catalogue.
 * <p>
 * Suppliers create and maintain product definitions here.
 * Warehouse managers browse and search this catalogue when raising Purchase Orders.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/product-master")
@RequiredArgsConstructor
@Tag(name = "6. Product Master Catalogue", description = "Global product definitions. Browsed by managers, maintained by suppliers.")
public class ProductMasterController {

    private final ProductMasterService productMasterService;

    @Operation(
            summary = "Create a new product definition",
            description = "Supplier or Admin creates a global product entry. Barcode must be unique if provided."
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ProductMasterResponse> create(@Valid @RequestBody ProductMasterRequest request) {
        return new ResponseEntity<>(productMasterService.create(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update a product definition",
            description = "Updates name, description, barcode, or physical specs of an existing product definition."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ProductMasterResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ProductMasterRequest request) {
        return ResponseEntity.ok(productMasterService.update(id, request));
    }

    @Operation(summary = "Get product definition by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductMasterResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(productMasterService.getById(id));
    }

    @Operation(
            summary = "Search the global product catalogue",
            description = "Fuzzy search by product name or barcode. Used by managers when starting a new PO."
    )
    @GetMapping("/search")
    public ResponseEntity<Page<ProductMasterResponse>> search(
            @Parameter(description = "Optional search term — name or barcode") @RequestParam(required = false) String query,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(productMasterService.search(query, pageable));
    }
}
