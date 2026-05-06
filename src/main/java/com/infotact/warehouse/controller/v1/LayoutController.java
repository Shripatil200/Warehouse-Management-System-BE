package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
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
 * REST controller for warehouse physical layout management.
 *
 * <p>
 * Tenant-safe design: All operations are scoped automatically
 * to the authenticated warehouse via JWT → TenantContext.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
@Tag(name = "5. Warehouse Layout", description = "Manage Zones, Aisles, and Storage Bins")
public class LayoutController {

    private final LayoutService layoutService;

    // ============================================================
    // READ: FULL LAYOUT
    // ============================================================

    /**
     * Retrieves full warehouse layout for current tenant.
     */
    @Operation(
            summary = "Get Warehouse Layout",
            description = "Returns hierarchical structure (Zones → Aisles → Bins) for current tenant"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layout retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WarehouseLayoutResponse> getLayout() {
        return ResponseEntity.ok(layoutService.getWarehouseLayout());
    }

    // ============================================================
    // READ: BINS
    // ============================================================

    @Operation(
            summary = "List Bins in Aisle",
            description = "Paginated retrieval of bins for a specific aisle"
    )
    @GetMapping("/aisle/{aisleId}/bins")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<WarehouseLayoutResponse.BinSummary>> getBinsByAisle(
            @Parameter(description = "Aisle UUID") @PathVariable String aisleId,
            @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(layoutService.getBinsByAisle(aisleId, pageable));
    }

    // ============================================================
    // BARCODE / SCAN
    // ============================================================

    @Operation(
            summary = "Get Bin Barcode",
            description = "Generates printable barcode image for bin"
    )
    @GetMapping(value = "/bin/{binId}/label", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> getBinLabel(
            @Parameter(description = "Bin UUID") @PathVariable String binId) {

        return ResponseEntity.ok(layoutService.getBinBarcode(binId));
    }

    @Operation(
            summary = "Verify Bin Scan",
            description = "Validates scanned barcode against expected bin"
    )
    @GetMapping("/bin/{binId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Boolean> verifyBinScan(
            @Parameter(description = "Expected bin UUID") @PathVariable String binId,
            @Parameter(description = "Scanned barcode value") @RequestParam String scannedCode) {

        return ResponseEntity.ok(layoutService.verifyBinScan(scannedCode, binId));
    }

    @Operation(
            summary = "Get Bin Code",
            description = "Returns human-readable bin code"
    )
    @GetMapping("/bin/{binId}/code")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<String> getBinCode(
            @Parameter(description = "Bin UUID") @PathVariable String binId) {

        return ResponseEntity.ok(layoutService.getBinCodeById(binId));
    }

    // ============================================================
    // STATUS MANAGEMENT
    // ============================================================

    @Operation(summary = "Update Zone Status")
    @PatchMapping("/zone/{zoneId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateZoneStatus(
            @PathVariable String zoneId,
            @RequestParam boolean active) {

        layoutService.updateZoneStatus(zoneId, active);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update Aisle Status")
    @PatchMapping("/aisle/{aisleId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateAisleStatus(
            @PathVariable String aisleId,
            @RequestParam boolean active) {

        layoutService.updateAisleStatus(aisleId, active);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update Bin Status")
    @PatchMapping("/bin/{binId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateBinStatus(
            @PathVariable String binId,
            @RequestParam boolean active) {

        layoutService.updateBinStatus(binId, active);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // CREATION
    // ============================================================

    @Operation(summary = "Create Zone")
    @PostMapping("/zone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addZone(@Valid @RequestBody ZoneRequest request) {

        layoutService.addZoneToWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Create Aisle")
    @PostMapping("/aisle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addAisle(@Valid @RequestBody AisleRequest request) {

        layoutService.addAisleToZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Bulk Create Bins")
    @PostMapping("/bins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkCreateBins(@Valid @RequestBody BulkBinRequest request) {

        layoutService.bulkCreateBins(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}