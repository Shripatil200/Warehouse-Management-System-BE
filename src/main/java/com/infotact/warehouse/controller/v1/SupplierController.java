package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.SupplierRequest;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import com.infotact.warehouse.service.SupplierManagementService;
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

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "5. Supplier Management", description = "Admin-managed supplier CRUD operations")
public class SupplierController {

    private final SupplierManagementService supplierManagementService;

    @Operation(summary = "Create a new supplier", description = "Admin or Manager creates a supplier record.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Supplier created"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate contact/email")
    })
    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        return new ResponseEntity<>(supplierManagementService.createSupplier(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a supplier")
    @PutMapping("/{supplierId}")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable String supplierId,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierManagementService.updateSupplier(supplierId, request));
    }

    @Operation(summary = "List all suppliers", description = "Paginated list of all suppliers.")
    @GetMapping
    public ResponseEntity<Page<SupplierResponse>> getAllSuppliers(
            @ParameterObject @PageableDefault(size = 20, sort = "companyName") Pageable pageable) {
        return ResponseEntity.ok(supplierManagementService.getAllSuppliers(pageable));
    }

    @Operation(summary = "Get supplier by ID")
    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierResponse> getById(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierManagementService.getById(supplierId));
    }

    @Operation(summary = "Deactivate a supplier")
    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Void> deactivateSupplier(@PathVariable String supplierId) {
        supplierManagementService.deactivateSupplier(supplierId);
        return ResponseEntity.noContent().build();
    }
}
