package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.BinRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * INTERNAL SECURITY CHECK
     * Ensures only a SUPER_ADMIN can access global warehouse management.
     */
    private void validateSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN"));

        if (!isSuperAdmin) {
            log.error("Unauthorized Access: User {} attempted to manage warehouse facilities.",
                    auth != null ? auth.getName() : "Anonymous");
            throw new UnauthorizedException("Access Denied: Only Super Admins can manage warehouse sites.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive) {
        validateSuperAdmin(); // Locked

        Page<Warehouse> warehouses = includeInactive
                ? warehouseRepository.findAll(pageable)
                : warehouseRepository.findAllByActiveTrue(pageable);

        return warehouses.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(String id) {
        validateSuperAdmin(); // Locked

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse Not Found"));
        return mapToResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(String id, WarehouseRequest request) {
        validateSuperAdmin(); // Locked

        Warehouse warehouse = warehouseRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active Warehouse not found"));

        if (!warehouse.getName().equalsIgnoreCase(request.name()) &&
                warehouseRepository.existsByNameIgnoreCase(request.name())) {
            throw new AlreadyExistsException("Warehouse name already exists");
        }

        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        return mapToResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public void activateWarehouse(String id) {
        validateSuperAdmin(); // Locked
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public void deactivateWarehouse(String id) {
        validateSuperAdmin(); // Locked
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        validateSuperAdmin(); // Locked

        if (warehouseRepository.existsByNameIgnoreCase(request.name())) {
            throw new AlreadyExistsException("Warehouse name already exists");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        warehouse.setActive(true);

        return mapToResponse(warehouseRepository.save(warehouse));
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