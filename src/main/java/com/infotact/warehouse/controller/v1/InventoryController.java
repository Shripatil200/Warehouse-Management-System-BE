package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling inventory-related operations such as
 * receiving shipments and stock adjustments.
 */
@RestController
@RequestMapping(path="/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Controller", description = "Endpoints for managing warehouse stock")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Processes an incoming shipment and updates the inventory records.
     * * @param request The shipment details including items, quantities, and origin.
     * @return 204 No Content on success.
     */
    @Operation(
            summary = "Receive a new shipment",
            description = "Validates the incoming request and triggers the background inventory update logic."
    )
    @ApiResponse(responseCode = "204", description = "Shipment processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request body (Validation failed)")
    @PostMapping(path="/receive")
    public ResponseEntity<Void> receiveShipment(@Valid @RequestBody ReceivingRequest request){
        inventoryService.receiveShipment(request);
        return ResponseEntity.noContent().build();
    }
}