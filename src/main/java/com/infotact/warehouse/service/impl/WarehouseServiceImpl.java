package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive) {
        Page<Warehouse> warehouses;
        if (includeInactive) {
            // Admin View: See everything for auditing
            warehouses = warehouseRepository.findAll(pageable);
        } else {
            // Operations View: Only active warehouse
            warehouses = warehouseRepository.findAllByActiveTrue(pageable);
        }
        return warehouses.map(this::mapToResponse);
    }


    @Override
    public WarehouseResponse getWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Warehouse Not Found"));

        return mapToResponse(warehouse);
    }


    @Override
    @Transactional // Required for data integrity in updates
    public WarehouseResponse updateWarehouse(String id, WarehouseRequest request) {
        // 1. Find the warehouse (ensure it is also active)
        Warehouse warehouse = warehouseRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active Warehouse with id: " + id + " not found"));

        // 2. Business Rule: Unique Name Check
        // If name is changing, ensure new name isn't taken by another warehouse
        if (!warehouse.getName().equalsIgnoreCase(request.name()) &&
                warehouseRepository.existsByNameIgnoreCase(request.name())) {
            throw new AlreadyExistsException("Warehouse with name " + request.name() + " already exists");
        }

        // 3. Update all fields from the Request Record
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());

        // 4. Save and Map
        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        log.info("refactor: updated warehouse details for ID: {}", id); // Semantic logging

        return mapToResponse(updatedWarehouse);
    }

    @Override
    public void activateWarehouse(String id) {

    }

    @Override
    public void deactivateWarehouse(String id) {

    }

    @Override
    public WarehouseLayoutResponse getWarehouseLayout(String id) {
        return null;
    }

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        log.info("creating warehouse with name: {}", request.name());
        // 1. Validation: Prevent duplicate warehouses
        if (warehouseRepository.existsByNameIgnoreCase(request.name())) {
            throw new AlreadyExistsException("Warehouse with name: " + request.name() + " already exists");
        }

        // 2. Mapping: Transform Request Record to Entity
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        warehouse.setActive(true);

        // 3. Persistence: Save to PostgreSQL
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        // 4. Audit: Log the action for the "immutable ledger" [cite: 155]
        log.info("feat: created new warehouse facility: {}", savedWarehouse.getName());

        return mapToResponse(savedWarehouse);
    }



    private WarehouseResponse mapToResponse(Warehouse entity) {

        return WarehouseResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .location(entity.getLocation())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .zoneCount(entity.getZones() != null ? entity.getZones().size() : 0)
                .build();
    }
}
