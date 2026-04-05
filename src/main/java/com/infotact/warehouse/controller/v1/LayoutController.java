package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.service.LayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
@Tag(name = "Warehouse Layout", description = "Operations for managing the physical structure: Zones, Aisles, and Bins")
public class LayoutController {

    private final LayoutService layoutService;

    @Operation(summary = "Get full warehouse layout", description = "Returns a hierarchical tree. Accessible by Admin and Manager.")
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WarehouseLayoutResponse> getFullLayout(
            @Parameter(description = "The UUID of the warehouse") @PathVariable String warehouseId) {
        return ResponseEntity.ok(layoutService.getWarehouseLayout(warehouseId));
    }

    @Operation(summary = "List bins in an aisle", description = "Retrieves a paginated list of storage bins. Accessible by Admin and Manager.")
    @GetMapping("/aisle/{aisleId}/bins")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<WarehouseLayoutResponse.BinSummary>> getBinsByAisle(
            @Parameter(description = "The UUID of the aisle") @PathVariable String aisleId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(layoutService.getBinsByAisle(aisleId, pageable));
    }

    @Operation(summary = "Add a zone to a warehouse", description = "Restricted to Admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zone added successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only Admin can add zones")
    })
    @PostMapping(path = "/zone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addZoneToWarehouse(@Valid @RequestBody ZoneRequest request) {
        layoutService.addZoneToWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Add an aisle to a zone", description = "Restricted to Admin.")
    @PostMapping(path = "/aisle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addAisleToZone(@Valid @RequestBody AisleRequest request){
        layoutService.addAisleToZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Bulk create storage bins", description = "Restricted to Admin.")
    @PostMapping(path = "/storage_bin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkCreateBins(@Valid @RequestBody BulkBinRequest request){
        layoutService.bulkCreateBins(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}