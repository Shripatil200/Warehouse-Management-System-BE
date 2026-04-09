package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.dto.v1.response.PurchaseOrderResponse;
import com.infotact.warehouse.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Purchase Orders (Inbound Stock).
 * <p>
 * This controller manages the documentation and status of stock arriving from suppliers.
 * It serves as the prerequisite for the 'Receiving' process. All data is partitioned
 * by the authenticated manager's warehouse.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "7. Purchase Orders", description = "Management of inbound stock expectations from suppliers")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    /**
     * Registers a new Purchase Order in the system.
     * <p>
     * Logic: Creates an expectation of stock. The PO must reference an existing
     * supplier and valid product SKUs. The initial status is always 'PLACED'.
     * </p>
     *
     * @param request Data containing supplier ID and the list of items/quantities.
     * @return The created Purchase Order details.
     */
    @Operation(
            summary = "Create a Purchase Order",
            description = "Registers a new inbound order. This document is required before inventory can be received."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "PO created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid supplier ID or product SKUs"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Access restricted to Managers")
    })
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        log.info("REST request to create Purchase Order for supplier: {}", request.supplierId());
        return new ResponseEntity<>(poService.createPurchaseOrder(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves full details of a specific Purchase Order.
     *
     * @param id The unique identifier (UUID) of the PO.
     * @return Purchase order details including line items and current status.
     */
    @Operation(summary = "Get PO by ID", description = "Retrieves full details of a specific purchase order using its UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrder(
            @Parameter(description = "The UUID of the purchase order", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return ResponseEntity.ok(poService.getPurchaseOrder(id));
    }

    /**
     * Retrieves all Purchase Orders associated with the manager's warehouse.
     * <p>
     * Logic: Automatically filters by the authenticated user's warehouse context.
     * </p>
     *
     * @param status Optional filter to view POs by state (e.g., PLACED, RECEIVED, PARTIAL).
     * @return A list of matching Purchase Orders.
     */
    @Operation(summary = "List all POs", description = "Retrieves all inbound orders for the current warehouse. Supports status filtering.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PurchaseOrderResponse.class))))
    })
    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> getAllPurchaseOrders(
            @Parameter(description = "Optional status filter", example = "PLACED")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(poService.getAllPurchaseOrders(status));
    }
}