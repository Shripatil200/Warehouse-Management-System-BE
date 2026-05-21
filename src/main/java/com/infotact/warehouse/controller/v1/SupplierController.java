package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.SupplierProfileUpdateRequest;
import com.infotact.warehouse.dto.v1.request.SupplierRegistrationRequest;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import com.infotact.warehouse.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * REST controller for supplier identity management.
 * Handles self-registration and profile management for independent suppliers.
 */
@RestController
@RequestMapping("/api/v1/supplier")
@RequiredArgsConstructor
@Tag(name = "5. Supplier Management", description = "Supplier registration and profile management")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(
            summary = "Supplier self-registration",
            description = "Public endpoint. Suppliers create their own accounts, independent of any warehouse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Supplier account created"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody SupplierRegistrationRequest request) {
        return new ResponseEntity<>(supplierService.register(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get my supplier profile", description = "Authenticated supplier retrieves their own profile.")
    @GetMapping("/me")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierResponse> getMyProfile() {
        return ResponseEntity.ok(supplierService.getMyProfile());
    }

    @Operation(summary = "Update my supplier profile", description = "Authenticated supplier updates their own profile.")
    @PutMapping("/me")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<String> updateMyProfile(@Valid @RequestBody SupplierProfileUpdateRequest request) {
        return ResponseEntity.ok(supplierService.updateMyProfile(request));
    }

    @Operation(
            summary = "List all suppliers",
            description = "Paginated list of all active suppliers. Available to warehouse MANAGER and ADMIN for PO creation."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<SupplierResponse>> getAllSuppliers(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(supplierService.getAllSuppliers(pageable));
    }

    @Operation(summary = "Get supplier by ID", description = "Retrieves the public profile of a specific supplier.")
    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SupplierResponse> getById(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierService.getById(supplierId));
    }
}
