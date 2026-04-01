package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping(path="/receive")
    public ResponseEntity<Void> receiveShipment(@Valid @RequestBody ReceivingRequest request){
        inventoryService.receiveShipment(request);
        return ResponseEntity.noContent().build();
    }


}
