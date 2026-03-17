package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
public class LayoutController {

    private final WarehouseService warehouseService;


    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<WarehouseLayoutResponse> getFullLayout(@PathVariable String warehouseId) {
        return ResponseEntity.ok(warehouseService.getWarehouseLayout(warehouseId));
    }

    // Add the operational (paginated) view here
    @GetMapping("/aisle/{aisleId}/bins")
    public ResponseEntity<Page<WarehouseLayoutResponse.BinSummary>> getBinsByAisle(
            @PathVariable String aisleId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(warehouseService.getBinsByAisle(aisleId, pageable));
    }
}
