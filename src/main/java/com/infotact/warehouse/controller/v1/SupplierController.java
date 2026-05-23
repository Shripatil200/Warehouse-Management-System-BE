package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.SupplierLoginRequest;
import com.infotact.warehouse.dto.v1.request.SupplierProfileUpdateRequest;
import com.infotact.warehouse.dto.v1.request.SupplierRegistrationRequest;
import com.infotact.warehouse.dto.v1.response.AuthResponse;
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

@RestController
@RequestMapping("/api/v1/supplier")
@RequiredArgsConstructor
@Tag(name = "5. Supplier Management", description = "Supplier registration, login, and profile management")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(
            summary = "Supplier self-registration",
            description = "Public endpoint. Suppliers create their own accounts independently of any warehouse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Supplier account created"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody SupplierRegistrationRequest request) {
        return new ResponseEntity<>(supplierService.register(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Supplier login",
            description = "Public endpoint. Authenticates a supplier and returns a JWT. " +
                    "Use this token on all /api/v1/supplier-catalogue and /api/v1/product-master (POST/PUT) endpoints."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody SupplierLoginRequest request) {
        return ResponseEntity.ok(supplierService.login(request));
    }

    @Operation(summary = "Get my profile", description = "Authenticated supplier retrieves their own profile.")
    @GetMapping("/me")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierResponse> getMyProfile() {
        return ResponseEntity.ok(supplierService.getMyProfile());
    }

    @Operation(summary = "Update my profile", description = "Authenticated supplier updates their own profile. All fields optional.")
    @PutMapping("/me")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<String> updateMyProfile(@Valid @RequestBody SupplierProfileUpdateRequest request) {
        return ResponseEntity.ok(supplierService.updateMyProfile(request));
    }

    @Operation(
            summary = "List all suppliers",
            description = "Paginated list of all suppliers. Available to warehouse MANAGER and ADMIN for PO creation."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<SupplierResponse>> getAllSuppliers(
            @ParameterObject @PageableDefault(size = 20, sort = "companyName") Pageable pageable) {
        return ResponseEntity.ok(supplierService.getAllSuppliers(pageable));
    }

    @Operation(summary = "Get supplier by ID", description = "Fetches public profile of a specific supplier.")
    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SupplierResponse> getById(@PathVariable String supplierId) {
        return ResponseEntity.ok(supplierService.getById(supplierId));
    }
}
