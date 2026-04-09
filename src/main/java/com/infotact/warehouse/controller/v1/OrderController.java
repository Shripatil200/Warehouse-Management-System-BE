package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
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
 * This controller handles the lifecycle of sales orders, from creation to status tracking.
 * All operations are scoped to the authenticated user's assigned warehouse, preventing
 * cross-facility data leakage.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "6. Order Management", description = "Endpoints for managing outbound customer orders and fulfillment")
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new outbound order for the warehouse.
     * <p>
     * Logic: Accepts a list of product SKUs and quantities. The system verifies
     * stock availability and anchors the order to the requester's warehouse.
     * </p>
     *
     * @param request Data containing order number and items.
     * @return The created order details with a generated UUID.
     */
    @Operation(summary = "Create an outbound order",
            description = "Initializes a new customer order. Stock is validated against the warehouse inventory.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid SKU or insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient permissions")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves specific order details by ID.
     *
     * @param id The unique identifier of the order.
     * @return Order details including items and fulfillment status.
     */
    @Operation(summary = "Get order by ID", description = "Retrieves full details of a specific outbound order.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "The UUID of the order", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    /**
     * Lists orders associated with the current warehouse.
     * <p>
     * Logic: Automatically filters by the authenticated user's warehouse ID.
     * Results can be optionally filtered by status (e.g., PENDING, SHIPPED).
     * </p>
     *
     * @param status Optional filter for order status.
     * @return A list of orders matching the criteria.
     */
    @Operation(summary = "List all warehouse orders",
            description = "Returns all outbound orders for the requester's warehouse. Can be filtered by status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of orders retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<OrderResponse>> getOrders(
            @Parameter(description = "Filter by order status", example = "PENDING")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getWarehouseOrders(status));
    }
}