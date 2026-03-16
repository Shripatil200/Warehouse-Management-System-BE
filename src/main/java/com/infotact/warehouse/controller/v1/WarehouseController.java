package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path="/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request){
        log.info("REST request to create Warehouse: {}", request.name());

        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
    }

    @GetMapping
    public ResponseEntity<Page<WarehouseResponse>> getAllWarehouses(
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ){
        return ResponseEntity.ok(warehouseService.getAllWarehouses(pageable, includeInactive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouse(@PathVariable String id){
        return ResponseEntity.ok(warehouseService.getWarehouse(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(@PathVariable String id, @Valid @RequestBody WarehouseRequest request){
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateWarehouse(@PathVariable String id){
        warehouseService.activateWarehouse(id);
        return ResponseEntity.noContent().build(); // 204 is standard for status changes
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateWarehouse(@PathVariable String id) {
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/layout")
    public ResponseEntity<WarehouseLayoutResponse> getFullLayout(@PathVariable String id) {
        return ResponseEntity.ok(warehouseService.getWarehouseLayout(id));
    }
}
