package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
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
 * REST controller for warehouse-scoped warehouse management.
 *
 * <p>
 * This controller enforces strict warehouse-scoped isolation.
 * All operations (except setup) are automatically scoped to the
 * authenticated user's warehouse via JWT and WarehouseContext.
 * </p>
 *
 * <p>
 * <b>Design Principle:</b> No API accepts warehouseId as input.
 * The system determines warehouse context internally.
 * </p>
 */
@RestController
@RequestMapping(path = "/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "0. Warehouse Management", description = "Tenant-scoped operations for warehouse lifecycle and setup")
public class WarehouseController {

    private final WarehouseService warehouseService;

    // ============================================================
    // SYSTEM SETUP (PUBLIC)
    // ============================================================

    /**
     * Initializes the system with a warehouse and primary admin account.
     *
     * <p>
     * This is a public endpoint used during first-time onboarding.
     * It creates:
     * <ul>
     *     <li>Warehouse entity</li>
     *     <li>Primary Admin user</li>
     * </ul>
     * </p>
     *
     * @param request onboarding payload containing warehouse + admin details
     * @return created warehouse profile
     */
    @Operation(
            summary = "Initial System Setup",
            description = "Bootstrap endpoint to create the first warehouse and its Admin user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Warehouse created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or resource already exists")
    })
    @PostMapping("/setup")
    public ResponseEntity<WarehouseResponse> setup(
            @Valid @RequestBody CreateWarehouseRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(warehouseService.createWarehouse(request));
    }

    // ============================================================
    // CURRENT TENANT OPERATIONS
    // ============================================================

    /**
     * Retrieves the warehouse associated with the current authenticated user.
     *
     * @return tenant-specific warehouse details
     */
    @Operation(
            summary = "Get Current Warehouse",
            description = "Returns the warehouse linked to the authenticated user (tenant-safe)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warehouse fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/me")
    public ResponseEntity<WarehouseResponse> getCurrentWarehouse() {
        return ResponseEntity.ok(warehouseService.getCurrentWarehouse());
    }

    /**
     * Updates metadata of the current warehouse.
     *
     * <p>
     * Only users with ADMIN role are authorized.
     * </p>
     *
     * @param request updated warehouse details
     * @return updated warehouse response
     */
    @Operation(
            summary = "Update Warehouse",
            description = "Updates the name and location of the current warehouse"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warehouse updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied (Admin only)")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @Valid @RequestBody WarehouseRequest request) {

        return ResponseEntity.ok(warehouseService.updateWarehouse(request));
    }

    /**
     * Activates the current warehouse.
     *
     * <p>
     * Restores operational state.
     * </p>
     */
    @Operation(
            summary = "Activate Warehouse",
            description = "Marks the current warehouse as ACTIVE"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Warehouse activated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied (Admin only)")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/activate")
    public ResponseEntity<Void> activateWarehouse() {
        warehouseService.activateWarehouse();
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivates the current warehouse.
     *
     * <p>
     * This is a soft-delete operation.
     * All operations will be blocked for this warehouse.
     * </p>
     */
    @Operation(
            summary = "Deactivate Warehouse",
            description = "Performs soft delete by deactivating the current warehouse"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Warehouse deactivated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied (Admin only)")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> deactivateWarehouse() {
        warehouseService.deactivateWarehouse();
        return ResponseEntity.noContent().build();
    }
}