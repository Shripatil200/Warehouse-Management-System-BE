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


@Slf4j
@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive) {
        Page<Warehouse> warehouses;
        if (includeInactive) {
            warehouses = warehouseRepository.findAll(pageable);
        } else {
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
    public WarehouseResponse updateWarehouse(String id, WarehouseRequest request) {
        return null;
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
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        return null;
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
