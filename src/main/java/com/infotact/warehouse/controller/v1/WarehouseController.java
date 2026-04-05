package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Slf4j
@RestController
@RequestMapping(path="/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse Facilities", description = "Global operations for managing physical warehouse locations. Restricted to Super Admins.")
@SecurityRequirement(name = "bearerAuth") // Tells Swagger this needs a JWT
@PreAuthorize("hasRole('SUPER_ADMIN')")   // The security gate
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Register a new warehouse", description = "Creates a new physical facility site. Names must be unique. Restricted to SUPER_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Warehouse created successfully"),
            @ApiResponse(responseCode = "403", description = "Access Denied: Only Super Admin permitted"),
            @ApiResponse(responseCode = "409", description = "Warehouse name already exists")
    })
    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request){
        log.info("REST request by SuperAdmin to create Warehouse: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    @Operation(summary = "List all warehouses", description = "Retrieves a paginated list of all facilities globally.")
    @GetMapping
    public ResponseEntity<Page<WarehouseResponse>> getAllWarehouses(
            @ParameterObject Pageable pageable,
            @Parameter(description = "If true, includes inactive/deactivated warehouses")
            @RequestParam(defaultValue = "false") boolean includeInactive
    ){
        return ResponseEntity.ok(warehouseService.getAllWarehouses(pageable, includeInactive));
    }

    @Operation(summary = "Get warehouse by ID", description = "Retrieves specific details for a single warehouse facility.")
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouse(
            @Parameter(description = "The UUID of the warehouse") @PathVariable String id){
        return ResponseEntity.ok(warehouseService.getWarehouse(id));
    }

    @Operation(summary = "Update warehouse details", description = "Updates the name and geographic location of an active warehouse.")
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable String id,
            @Valid @RequestBody WarehouseRequest request){
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
    }

    @Operation(summary = "Activate a warehouse", description = "Restores a deactivated warehouse to active status.")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateWarehouse(@PathVariable String id){
        warehouseService.activateWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a warehouse", description = "Performs a soft-delete by deactivating the facility.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateWarehouse(@PathVariable String id) {
        log.warn("REST request by SuperAdmin to deactivate warehouse: {}", id);
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}