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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the physical architecture of the warehouse.
 * <p>
 * This controller provides endpoints to define and modify the hierarchical structure
 * of a facility (Zones, Aisles, and Bins). Restricting access to authorized personnel
 * ensures the integrity of the facility's digital twin.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
@Tag(name = "5. Warehouse Layout", description = "Endpoints for modeling and maintaining the physical facility structure")
public class LayoutController {

    private final LayoutService layoutService;

    /**
     * Retrieves the scannable barcode for a specific bin.
     * <p>
     * <b>Industry Usage:</b> Allows a manager to click a bin on the digital twin UI
     * and immediately download/print the physical label for the shelf.
     * </p>
     * @param binId The UUID of the storage bin.
     * @return 200 OK with PNG image data, or 404 if not found.
     */
    @Operation(summary = "Get Bin Label", description = "Generates a printable 1D barcode image for a specific bin identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barcode image generated successfully"),
            @ApiResponse(responseCode = "404", description = "Bin ID not found")
    })
    @GetMapping(value = "/bin/{binId}/label", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> getBinLabel(
            @Parameter(description = "UUID of the storage bin", required = true)
            @PathVariable String binId) {
        return ResponseEntity.ok(layoutService.getBinBarcode(binId));
    }

    /**
     * Verifies if a physical scan matches the expected bin.
     * <p>
     * <b>Process Security:</b> Used by mobile Android/iOS apps to confirm a worker
     * is standing at the correct location before picking an item.
     * </p>
     * @param binId The expected Bin UUID.
     * @param scannedCode The text value read by the barcode scanner.
     * @return 200 OK with boolean indicating match status.
     */
    @Operation(summary = "Verify Bin Scan", description = "Validates a physical barcode scan against a digital bin record.")
    @GetMapping("/bin/{binId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Boolean> verifyBinScan(
            @Parameter(description = "UUID of the bin being visited") @PathVariable String binId,
            @Parameter(description = "Raw text from the scanner laser") @RequestParam String scannedCode) {
        return ResponseEntity.ok(layoutService.verifyBinScan(scannedCode, binId));
    }

    /**
     * Retrieves the complete hierarchical tree of a specific warehouse.
     * <p>
     * <b>Usage:</b> Ideal for initializing frontend warehouse maps or facility-wide audits[cite: 2].
     * </p>
     */
    @Operation(summary = "Get full warehouse layout", description = "Returns a recursive tree structure (Zones > Aisles > Bins) including capacity.")
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WarehouseLayoutResponse> getFullLayout(
            @Parameter(description = "UUID of the warehouse facility") @PathVariable String warehouseId) {
        return ResponseEntity.ok(layoutService.getWarehouseLayout(warehouseId));
    }

    /**
     * Lists bins for a specific aisle with pagination.
     */
    @Operation(summary = "List bins in an aisle", description = "Paginated retrieval of storage slots for a specific aisle/row.")
    @GetMapping("/aisle/{aisleId}/bins")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<WarehouseLayoutResponse.BinSummary>> getBinsByAisle(
            @Parameter(description = "UUID of the parent aisle") @PathVariable String aisleId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(layoutService.getBinsByAisle(aisleId, pageable));
    }

    // --- Status/Maintenance Endpoints ---

    @Operation(summary = "Update Zone status", description = "Toggle zone availability for maintenance.")
    @PatchMapping("/zone/{zoneId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateZoneStatus(
            @Parameter(description = "UUID of the zone") @PathVariable String zoneId,
            @RequestParam boolean active) {
        layoutService.updateZoneStatus(zoneId, active);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update Aisle status", description = "Toggle aisle availability (e.g., rack repair)[cite: 2].")
    @PatchMapping("/aisle/{aisleId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateAisleStatus(
            @Parameter(description = "UUID of the aisle") @PathVariable String aisleId,
            @RequestParam boolean active) {
        layoutService.updateAisleStatus(aisleId, active);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update Bin status", description = "Toggle individual bin availability (e.g., broken shelf).")
    @PatchMapping("/bin/{binId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateBinStatus(
            @Parameter(description = "UUID of the bin") @PathVariable String binId,
            @RequestParam boolean active) {
        layoutService.updateBinStatus(binId, active);
        return ResponseEntity.ok().build();
    }

    // --- Management/Creation Endpoints ---

    @Operation(summary = "Add a zone to a warehouse", description = "Creates a new functional area like 'Receiving' or 'Cold Storage'[cite: 2].")
    @PostMapping(path = "/zone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addZoneToWarehouse(@Valid @RequestBody ZoneRequest request) {
        layoutService.addZoneToWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Add an aisle to a zone", description = "Adds a physical row identifier to a specific zone[cite: 2].")
    @PostMapping(path = "/aisle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addAisleToZone(@Valid @RequestBody AisleRequest request){
        layoutService.addAisleToZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Bulk create storage bins", description = "Provision multiple storage slots automatically via prefix and sequence (max 999)[cite: 2].")
    @PostMapping(path = "/storage_bin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkCreateBins(@Valid @RequestBody BulkBinRequest request){
        layoutService.bulkCreateBins(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Resolves a human-readable bin code from a bin UUID.
     * <p>
     * <b>Usage:</b> Used by mobile apps to display the physical location name
     * (e.g., "ZONE-A-01-005") to a picker who only has the digital ID.
     * </p>
     */
    @Operation(summary = "Get human-readable bin code", description = "Resolves a Bin UUID to its physical label string.")
    @GetMapping("/bin/{binId}/code")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<String> getBinCode(
            @Parameter(description = "UUID of the storage bin") @PathVariable String binId) {
        return ResponseEntity.ok(layoutService.getBinCodeById(binId));
    }
}