package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.service.LayoutService;
import com.infotact.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/layouts")
@RequiredArgsConstructor
public class LayoutController {



    private final LayoutService layoutService;


    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<WarehouseLayoutResponse> getFullLayout(@PathVariable String warehouseId) {
        return ResponseEntity.ok(layoutService.getWarehouseLayout(warehouseId));
    }

    // Add the operational (paginated) view here
    @GetMapping("/aisle/{aisleId}/bins")
    public ResponseEntity<Page<WarehouseLayoutResponse.BinSummary>> getBinsByAisle(
            @PathVariable String aisleId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(layoutService.getBinsByAisle(aisleId, pageable));
    }


    @PostMapping(path = "/zone")
    public ResponseEntity<Void> addZoneToWarehouse(@Valid @RequestBody ZoneRequest request) {
        layoutService.addZoneToWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(path = "/aisle")
    public ResponseEntity<Void> addAisleToZone(@Valid @RequestBody AisleRequest request){
        layoutService.addAisleToZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(path = "/storage_bin")
    public ResponseEntity<Void> bulkCreateBins(@Valid @RequestBody BulkBinRequest request){
        layoutService.bulkCreateBins(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}
