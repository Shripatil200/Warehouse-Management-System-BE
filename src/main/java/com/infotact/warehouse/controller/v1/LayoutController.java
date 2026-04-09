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

/**
 * REST controller for managing the physical architecture of the warehouse.
 * <p>
 * This controller provides endpoints to define and modify the hierarchical structure
 * of a facility (Zones, Aisles, and Bins). It supports operational maintenance toggles
 * that allow administrators to "close" specific areas of the warehouse for repairs
 * or inventory audits without deleting the underlying structural data.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
@Tag(name = "5. Warehouse Layout", description = "Endpoints for modeling and maintaining the physical facility structure")
public class LayoutController {

    private final LayoutService layoutService;

    /**
     * Retrieves the complete hierarchical tree of a specific warehouse.
     * <p>
     * <b>Usage:</b> Ideal for initializing frontend warehouse maps or facility-wide audits.
     * It performs a "deep fetch" of all child components.
     * </p>
     *
     * @param warehouseId The UUID of the warehouse to visualize.
     * @return A recursive tree representing the entire facility.
     */
    @Operation(
            summary = "Get full warehouse layout",
            description = "Returns a recursive tree structure (Zones > Aisles > Bins) including maintenance status and aggregated capacity."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Complete layout tree successfully generated"),
            @ApiResponse(responseCode = "404", description = "Warehouse ID not found in the system")
    })
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WarehouseLayoutResponse> getFullLayout(
            @Parameter(description = "UUID of the warehouse facility", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String warehouseId) {
        return ResponseEntity.ok(layoutService.getWarehouseLayout(warehouseId));
    }

    /**
     * Lists bins for a specific aisle with pagination.
     * <p>
     * <b>Optimization:</b> Prevents performance degradation when loading aisles
     * containing high-density storage (hundreds of bins).
     * </p>
     */
    @Operation(summary = "List bins in an aisle", description = "Paginated retrieval of storage slots for a specific aisle/row.")
    @GetMapping("/aisle/{aisleId}/bins")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<WarehouseLayoutResponse.BinSummary>> getBinsByAisle(
            @Parameter(description = "UUID of the parent aisle") @PathVariable String aisleId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(layoutService.getBinsByAisle(aisleId, pageable));
    }

    /**
     * Maintenance Toggle: Zone Level.
     * <p>
     * <b>Cascading Logic:</b> Setting active=false effectively blocks all child aisles
     * and bins from being selected for picking or putaway tasks.
     * </p>
     */
    @Operation(summary = "Update Zone status", description = "Toggle zone availability for maintenance or inventory counts.")
    @PatchMapping("/zone/{zoneId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateZoneStatus(
            @Parameter(description = "UUID of the zone") @PathVariable String zoneId,
            @Parameter(description = "True for operational, False for maintenance") @RequestParam boolean active) {
        layoutService.updateZoneStatus(zoneId, active);
        return ResponseEntity.ok().build();
    }

    /**
     * Maintenance Toggle: Aisle Level.
     */
    @Operation(summary = "Update Aisle status", description = "Toggle aisle availability (e.g., forklift obstruction or rack repair).")
    @PatchMapping("/aisle/{aisleId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateAisleStatus(
            @Parameter(description = "UUID of the aisle") @PathVariable String aisleId,
            @Parameter(description = "Status toggle") @RequestParam boolean active) {
        layoutService.updateAisleStatus(aisleId, active);
        return ResponseEntity.ok().build();
    }

    /**
     * Maintenance Toggle: Bin Level.
     */
    @Operation(summary = "Update Bin status", description = "Toggle individual bin availability (e.g., broken shelf or cleaning).")
    @PatchMapping("/bin/{binId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateBinStatus(
            @Parameter(description = "UUID of the bin") @PathVariable String binId,
            @Parameter(description = "Status toggle") @RequestParam boolean active) {
        layoutService.updateBinStatus(binId, active);
        return ResponseEntity.ok().build();
    }

    /**
     * Registers a new Warehouse Zone.
     * <p>
     * <b>Business Rule:</b> Zone names must be unique within the facility (e.g., 'Cold Storage').
     * </p>
     */
    @Operation(summary = "Add a zone to a warehouse", description = "Creates a new functional area (e.g., Hazardous, Receiving, Bulk).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zone registered successfully"),
            @ApiResponse(responseCode = "400", description = "Duplicate name or validation error")
    })
    @PostMapping(path = "/zone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addZoneToWarehouse(@Valid @RequestBody ZoneRequest request) {
        layoutService.addZoneToWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Registers a new Aisle within a Zone.
     */
    @Operation(summary = "Add an aisle to a zone", description = "Adds a physical row identifier to a specific zone.")
    @PostMapping(path = "/aisle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addAisleToZone(@Valid @RequestBody AisleRequest request){
        layoutService.addAisleToZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Automated Bin Provisioning.
     * <p>
     * <b>Logic:</b> Uses a naming prefix and sequence to rapidly onboard new aisles.
     * Prevents collisions by checking existing codes in the aisle range.
     * </p>
     */
    @Operation(summary = "Bulk create storage bins", description = "Provision multiple storage slots automatically via prefix and sequence (max 999).")
    @PostMapping(path = "/storage_bin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkCreateBins(@Valid @RequestBody BulkBinRequest request){
        layoutService.bulkCreateBins(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}