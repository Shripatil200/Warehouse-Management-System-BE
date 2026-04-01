package com.infotact.warehouse.controller.v1;

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
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path="/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse Facilities", description = "Operations for managing physical warehouse locations and status")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Register a new warehouse", description = "Creates a new physical facility site. Names must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Warehouse created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid facility data"),
            @ApiResponse(responseCode = "409", description = "Warehouse name already exists")
    })
    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request){
        log.info("REST request to create Warehouse: {}", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    @Operation(summary = "List all warehouses", description = "Retrieves a paginated list of facilities. Can include decommissioned sites.")
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

    @Operation(summary = "Activate a warehouse", description = "Restores a deactivated warehouse to active status for inventory operations.")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateWarehouse(@PathVariable String id){
        warehouseService.activateWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a warehouse", description = "Performs a soft-delete by deactivating the facility. Prevents new inventory operations.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateWarehouse(@PathVariable String id) {
        log.warn("REST request to deactivate warehouse: {}", id);
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}