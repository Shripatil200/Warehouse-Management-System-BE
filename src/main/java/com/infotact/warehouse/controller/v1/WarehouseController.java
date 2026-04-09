package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for root-level warehouse management.
 * <p>
 * This controller manages the lifecycle of the warehouse facility itself.
 * It includes the critical 'Setup' endpoint which initializes the system's
 * primary facility and its first Administrative user.
 * </p>
 */
@RestController
@RequestMapping(path="/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "0. Warehouse Management", description = "Root operations for facility setup and lifecycle control")
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * Initializes the system with a primary warehouse and an Admin account.
     * <p>
     * Logic: This is a <b>public</b> endpoint intended for first-time setup.
     * It creates the Warehouse entity and simultaneously generates the Super Admin
     * credentials required to manage the platform.
     * </p>
     *
     * @param request Data containing warehouse details and admin contact info.
     * @return The created warehouse profile.
     */
    @Operation(summary = "Initial System Setup",
            description = "Bootstrap endpoint to initialize the first warehouse facility and its primary Admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "System initialized successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or warehouse already exists")
    })
    @PostMapping("/setup")
    public ResponseEntity<WarehouseResponse> setup(@Valid @RequestBody CreateWarehouseRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    /**
     * Re-activates a previously deactivated warehouse.
     *
     * @param id The UUID of the warehouse.
     */
    @Operation(summary = "Activate Warehouse", description = "Restores a warehouse to ACTIVE status.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "The UUID of the warehouse", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id){
        warehouseService.activateWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivates a warehouse (Soft Delete).
     * <p>
     * Logic: Marks the warehouse as inactive. This prevents any new logins or
     * operations associated with this facility.
     * </p>
     */
    @Operation(summary = "Deactivate Warehouse", description = "Performs a soft delete by deactivating the facility.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates warehouse metadata (name, location, etc.).
     */
    @Operation(summary = "Update Warehouse", description = "Updates the physical details of the warehouse facility.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody WarehouseRequest request){
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
    }
}