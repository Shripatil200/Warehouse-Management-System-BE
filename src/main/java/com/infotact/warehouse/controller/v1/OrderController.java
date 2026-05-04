package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.request.PickVerificationRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.service.OrderService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing outbound customer orders.
 * <p>
 * This controller orchestrates the sales fulfillment lifecycle, including inventory
 * reservation, scan-verified picking, and status tracking. Access is restricted
 * to the warehouse associated with the authenticated user.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "6. SellingOrder Management", description = "Endpoints for managing outbound customer orders and fulfillment")
public class OrderController {

    private final OrderService orderService;

    /**
     * Initializes a new outbound order.
     * <p>
     * <b>Process:</b> Verifies stock availability and triggers FEFO-based soft-locks
     * on inventory layers to minimize waste.
     * </p>
     *
     * @param request Payload containing order identifier and SKU requirements.
     * @return The initialized order with reserved stock details.
     */
    @Operation(
            summary = "Create an outbound order",
            description = "Initializes a customer order and performs immediate FEFO inventory reservation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created and stock reserved"),
            @ApiResponse(responseCode = "400", description = "Invalid SKU or insufficient physical inventory"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient permissions to authorize stock movement")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves specific order details for a warehouse.
     *
     * @param id The unique identifier (UUID) of the order.
     * @return Full order details, including suggested bin locations for pickers.
     */
    @Operation(summary = "Get order by ID", description = "Retrieves full details including suggested pick locations.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "UUID of the order", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    /**
     * Lists orders scoped to the requester's assigned facility.
     *
     * @param status Optional filter for lifecycle stage (e.g., PENDING, PACKED).
     * @return A list of orders matching the facility and status criteria.
     */
    @Operation(
            summary = "List warehouse orders",
            description = "Returns orders for the requester's warehouse. Cross-warehouse access is strictly blocked."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @Parameter(description = "Filter by order status", example = "PENDING")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getWarehouseOrders(status));
    }

    /**
     * <b>Secure Fulfillment:</b> Validates physical picks via handheld scanners.
     * <p>
     * <b>Process:</b> This endpoint bridges the digital twin with physical reality.
     * It requires a match between the scanned SKU and the scanned bin code before
     * allowing the order to reach 'PACKED' status.
     * </p>
     *
     * @param request Payload containing physical scan data and quantity.
     * @return 200 OK if scans match digital records.
     */
    @Operation(
            summary = "Verify and Pack Order",
            description = "Validates physical barcode scans of Bin and SKU to ensure 100% picking accuracy."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pick verified and physical stock deducted"),
            @ApiResponse(responseCode = "400", description = "Scan mismatch: Incorrect product or location scanned")
    })
    @PostMapping("/verify-pack")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> verifyAndPack(@Valid @RequestBody PickVerificationRequest request) {
        orderService.verifyAndPack(request.getOrderId(), request.getScannedProductSku(), request.getScannedBinCode());
        return ResponseEntity.ok().build();
    }

    /**
     * Manages lifecycle stage transitions.
     * <p>
     * <b>Restriction:</b> Manual updates to 'PACKED' are blocked in the service layer
     * to force operators to use the physical scan verification endpoint.
     * </p>
     *
     * @param id The order UUID.
     * @param status The target lifecycle stage (e.g., SHIPPED, CANCELLED).
     * @return Updated order metadata.
     */
    @Operation(summary = "Update order status", description = "Standard status transitions (SHIPPED, CANCELLED).")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "Order UUID") @PathVariable String id,
            @Parameter(description = "Target status") @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}