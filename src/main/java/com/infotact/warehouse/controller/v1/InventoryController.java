package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.InventoryAdjustmentRequest;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing physical stock movements and inventory integrity.
 * <p>
 * Provides endpoints for high-concurrency stock acquisition, audit corrections,
 * and secure fulfillment verification.
 * </p>
 */
@RestController
@RequestMapping(path="/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "3. Inventory Management", description = "Endpoints for managing stock levels and movement verification")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Processes an incoming shipment.
     * <p>
     * <b>Process:</b> Triggers smart putaway logic and updates volumetric bin metrics.
     * </p>
     */
    @Operation(summary = "Receive shipment", description = "Registers inbound goods and identifies storage locations.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Shipment received and metrics updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient storage capacity")
    })
    @PostMapping(path="/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> receiveShipment(@Valid @RequestBody ReceivingRequest request){
        inventoryService.receiveShipment(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manual stock correction.
     * <p>
     * <b>Process:</b> Employs pessimistic locking to ensure audit accuracy during high-traffic.
     * </p>
     */
    @Operation(summary = "Adjust stock levels", description = "Manual correction for damaged, lost, or found items.")
    @PatchMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> adjustStock(@Valid @RequestBody InventoryAdjustmentRequest request) {
        inventoryService.adjustStock(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * <b>Industry-Ready Feature: Secure Fulfillment Pick.</b>
     * <p>
     * Used by mobile scanners to commit a pick only after physical verification.
     * Bridges the digital record with the physical barcode scans of both the bin and the SKU.
     * </p>
     */
    @Operation(
            summary = "Commit pick with verification",
            description = "Validates physical scans of Bin and SKU before decrementing digital inventory."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pick verified and committed"),
            @ApiResponse(responseCode = "400", description = "Scan mismatch: Incorrect Bin or Product scanned")
    })
    @PostMapping("/pick/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<Void> commitPickWithVerification(
            @Parameter(description = "Internal Inventory Item ID") @RequestParam String inventoryItemId,
            @Parameter(description = "Raw scan data from physical bin label") @RequestParam String scannedBinCode,
            @Parameter(description = "Raw scan data from product sticker") @RequestParam String scannedSku,
            @Parameter(description = "Physical quantity picked") @RequestParam Integer quantity) {

        inventoryService.commitPickWithVerification(inventoryItemId, scannedBinCode, scannedSku, quantity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> transferStock(@RequestParam String sourceItemId,
                                              @RequestParam String targetBinId,
                                              @RequestParam Integer quantity) {
        inventoryService.internalStockTransfer(sourceItemId, targetBinId, quantity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/replenish")
    @Operation(summary = "Manual Replenishment Trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> replenish(@RequestParam String productId,
                                          @RequestParam String targetBinId,
                                          @RequestParam Integer quantity) {
        inventoryService.replenishPickingFace(productId, targetBinId, quantity);
        return ResponseEntity.noContent().build();
    }
}