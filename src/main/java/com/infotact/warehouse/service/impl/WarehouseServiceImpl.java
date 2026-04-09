package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.WarehouseService;
import com.infotact.warehouse.util.EmailUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link WarehouseService} for facility lifecycle management.
 * <p>
 * This service implements a 'Single-Warehouse' provisioning tactic, anchoring
 * the SaaS multi-tenancy model. It orchestrates the simultaneous creation of
 * physical infrastructure (Warehouse) and the administrative identity (User)
 * required to manage it.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtils emailUtils;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li><b>Uniqueness Guard:</b> Validates both Warehouse Name and Admin Email
     * before initiating persistence.</li>
     * <li><b>Deterministic Passwords:</b> Sets the initial credential using the
     * <code>Welcome@ + last4</code> phone logic.</li>
     * <li><b>Resilient Onboarding:</b> Triggers an asynchronous welcome email.
     * Failures in the mail server will not roll back the facility creation.</li>
     * </ul>
     */
    @Override
    @Transactional
    public WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request) {

        if (warehouseRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AlreadyExistsException("Warehouse with name '" + request.getName() + "' already exists.");
        }

        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new AlreadyExistsException("The email '" + request.getAdminEmail() + "' is already registered.");
        }

        // 1. Create the Warehouse Facility
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setActive(true);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        // 2. Initialize the Primary Admin User
        User admin = new User();
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setContactNumber(request.getAdminContact());
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setWarehouse(savedWarehouse);

        // 3. Password Generation Logic
        String phone = request.getAdminContact();
        String lastFour = (phone != null && phone.length() >= 4)
                ? phone.substring(phone.length() - 4)
                : "0000";

        admin.setPassword(passwordEncoder.encode("Welcome@" + lastFour));
        userRepository.save(admin);

        // 4. Asynchronous Welcome Notification
        try {
            emailUtils.sendWarehouseWelcomeEmail(
                    admin.getEmail(),
                    admin.getName(),
                    savedWarehouse.getName()
            );
            log.info("Provisioning: Welcome email queued for Admin: {}", admin.getEmail());
        } catch (Exception e) {
            log.error("Warning: Warehouse created, but onboarding email failed: {}", e.getMessage());
        }

        log.info("SUCCESS: Facility '{}' initialized with Admin account '{}'.",
                savedWarehouse.getName(), admin.getEmail());

        return mapToResponse(savedWarehouse);
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void activateWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);
        log.info("Security: Warehouse {} has been activated.", id);
    }

    /**
     * {@inheritDoc}
     * <p><b>Note:</b> Deactivation effectively suspends all staff access to
     * the facility while preserving historical transaction data.</p>
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivateWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        log.warn("Security: Warehouse {} has been deactivated.", id);
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WarehouseResponse updateWarehouse(String id, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        return mapToResponse(warehouseRepository.save(warehouse));
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        return mapToResponse(warehouse);
    }

    /**
     * {@inheritDoc}
     * <p><b>Filtering:</b> Stream-based filtering is applied for the
     * <code>includeInactive</code> toggle.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive) {
        List<Warehouse> list = warehouseRepository.findAll();
        List<WarehouseResponse> responses = list.stream()
                .filter(w -> includeInactive || w.isActive())
                .map(this::mapToResponse)
                .toList();

        return new PageImpl<>(responses, pageable, responses.size());
    }

    /**
     * Maps the internal Warehouse entity to a builder-based Response DTO.
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