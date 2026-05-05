package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.VerifiedProof;
import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.VerifiedProofRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.WarehouseService;
import com.infotact.warehouse.util.EmailUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production-grade implementation of {@link WarehouseService}.
 *
 * <p>
 * Enforces strict tenant isolation: all operations are scoped to the
 * currently authenticated warehouse via {@link TenantContext}.
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
    private final VerifiedProofRepository proofRepo;

    // ============================================================
    // CREATE WAREHOUSE (PUBLIC ONBOARDING)
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * <p>
     * Performs secure onboarding:
     * - Validates email & contact proof tokens
     * - Ensures uniqueness of warehouse and admin identity
     * - Creates warehouse + admin user atomically
     * - Deletes verification tokens to prevent reuse
     * </p>
     */
    @Override
    @Transactional
    public WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request) {

        // 1. Validate Email Proof
        VerifiedProof emailProof = proofRepo.findById(request.getEmailToken())
                .orElseThrow(() -> new BadRequestException("Email verification expired or invalid."));

        if (!emailProof.getIdentifier().equalsIgnoreCase(request.getAdminEmail())) {
            throw new BadRequestException("Email mismatch with verification token.");
        }

        // 2. Validate Contact Proof
        VerifiedProof contactProof = proofRepo.findById(request.getContactToken())
                .orElseThrow(() -> new BadRequestException("Contact verification expired or invalid."));

        if (!contactProof.getIdentifier().equals(request.getAdminContact())) {
            throw new BadRequestException("Contact mismatch with verification token.");
        }

        // 3. Uniqueness Checks
        if (warehouseRepository.existsByNameIgnoreCaseAndLocationIgnoreCase(
                request.getName(), request.getLocation())) {
            throw new AlreadyExistsException("Warehouse already exists at this location.");
        }

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new AlreadyExistsException("Admin email already in use.");
        }

        if (userRepository.existsByContactNumber(request.getAdminContact())) {
            throw new AlreadyExistsException("Admin contact already in use.");
        }

        // 4. Create Warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setActive(true);

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        // 5. Create Admin User
        User admin = new User();
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setContactNumber(request.getAdminContact());
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setWarehouse(savedWarehouse);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(admin);

        // 6. Cleanup Proof Tokens
        proofRepo.delete(emailProof);
        proofRepo.delete(contactProof);

        log.info("Warehouse '{}' created with Admin '{}'", savedWarehouse.getName(), admin.getEmail());

        // 7. Send Welcome Email (non-blocking)
        try {
            emailUtils.sendWarehouseWelcomeEmail(
                    admin.getEmail(),
                    admin.getName(),
                    savedWarehouse.getName()
            );
        } catch (Exception e) {
            log.warn("Email failed for {}: {}", admin.getEmail(), e.getMessage());
        }

        return mapToResponse(savedWarehouse);
    }

    // ============================================================
    // TENANT-SCOPED OPERATIONS
    // ============================================================

    /**
     * {@inheritDoc}
     *
     * <p>
     * Retrieves the warehouse bound to the current authenticated user.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getCurrentWarehouse() {

        Warehouse warehouse = warehouseRepository.findById(getCurrentWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        return mapToResponse(warehouse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Updates metadata of the current tenant warehouse.
     * Only accessible by ADMIN users.
     * </p>
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WarehouseResponse updateWarehouse(@Valid WarehouseRequest request) {

        Warehouse warehouse = warehouseRepository.findById(getCurrentWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());

        return mapToResponse(warehouseRepository.save(warehouse));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Activates the current tenant warehouse.
     * </p>
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void activateWarehouse() {

        Warehouse warehouse = warehouseRepository.findById(getCurrentWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        warehouse.setActive(true);

        log.info("Warehouse {} activated", warehouse.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Deactivates the current tenant warehouse.
     * </p>
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivateWarehouse() {

        Warehouse warehouse = warehouseRepository.findById(getCurrentWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        warehouse.setActive(false);

        log.warn("Warehouse {} deactivated", warehouse.getId());
    }

    // ============================================================
    // INTERNAL UTILITIES
    // ============================================================

    /**
     * Retrieves the current tenant warehouse ID from {@link TenantContext}.
     */
    private String getCurrentWarehouseId() {
        String warehouseId = TenantContext.get();

        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalStateException("No tenant context found");
        }

        return warehouseId;
    }

    /**
     * Maps entity to response DTO.
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