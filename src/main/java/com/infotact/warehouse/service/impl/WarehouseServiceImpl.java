package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.BinRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link WarehouseService}.
 * Focuses on facility lifecycle management and ensuring unique naming conventions
 * for physical warehouse sites.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final BinRepository binRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive) {
        Page<Warehouse> warehouses;
        if (includeInactive) {
            // Administrative view for facility managers and auditors
            warehouses = warehouseRepository.findAll(pageable);
        } else {
            // Standard operations view; prevents scheduling stock into disabled facilities
            warehouses = warehouseRepository.findAllByActiveTrue(pageable);
        }
        return warehouses.map(this::mapToResponse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse Not Found"));

        return mapToResponse(warehouse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(String id, WarehouseRequest request) {
        // Validation: Updates are only permitted on active warehouse records
        Warehouse warehouse = warehouseRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active Warehouse with id: " + id + " not found"));

        // Business Rule: Enforce facility name uniqueness during name changes
        if (!warehouse.getName().equalsIgnoreCase(request.name()) &&
                warehouseRepository.existsByNameIgnoreCase(request.name())) {
            throw new AlreadyExistsException("Warehouse with name " + request.name() + " already exists");
        }

        warehouse.setName(request.name());
        warehouse.setLocation(request.location());

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        log.info("refactor: updated warehouse details for ID: {}", id);

        return mapToResponse(updatedWarehouse);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void activateWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse with id: " + id + " not found"));

        // Check current state to avoid unnecessary database IO (Idempotency)
        if (!warehouse.isActive()) {
            warehouse.setActive(true);
            warehouseRepository.save(warehouse);
            log.info("reactivated warehouse ID: {}", id);
        } else {
            log.warn("Warehouse with ID: {} is already active", id);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deactivateWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse with id: " + id + " not found"));

        // Soft-deactivation logic to prevent data loss while removing from operational list
        if (warehouse.isActive()) {
            warehouse.setActive(false);
            warehouseRepository.save(warehouse);
            log.info("deactivated warehouse ID: {}", id);
        } else {
            log.warn("Warehouse with ID: {} is already deactivated", id);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        log.info("creating warehouse with name: {}", request.name());

        // Prevent duplicate registration of physical sites
        if (warehouseRepository.existsByNameIgnoreCase(request.name())) {
            throw new AlreadyExistsException("Warehouse with name: " + request.name() + " already exists");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        warehouse.setActive(true);

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        log.info("feat: created new warehouse facility: {}", savedWarehouse.getName());

        return mapToResponse(savedWarehouse);
    }

    /**
     * Maps the Warehouse entity to a response DTO.
     * Includes a calculated 'zoneCount' derived from the child Zones collection.
     */
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