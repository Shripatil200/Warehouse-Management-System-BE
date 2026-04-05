package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.entity.PurchaseOrder;
import com.infotact.warehouse.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Access is strictly restricted to users with the MANAGER role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')") // Restricts all PO operations to Managers
@Tag(name = "Purchase Orders", description = "Management of inbound stock expectations from suppliers")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @Operation(summary = "Create a Purchase Order", description = "Registers a new order with a supplier. Status defaults to 'PLACED'.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "PO created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid supplier or product SKUs"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only Managers can create purchase orders")
    })
    @PostMapping
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        log.info("REST request to create Purchase Order for supplier: {}", request.supplierId());
        return new ResponseEntity<>(poService.createPurchaseOrder(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get PO by ID", description = "Retrieves the full details of a purchase order, including its items.")
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrder(@PathVariable String id) {
        return ResponseEntity.ok(poService.getPurchaseOrder(id));
    }

    @Operation(summary = "List all POs", description = "Retrieves a list of all purchase orders. Can be filtered by status.")
    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(poService.getAllPurchaseOrders(status));
    }
}