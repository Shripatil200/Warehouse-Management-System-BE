package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.VerifiedProof;
import com.infotact.warehouse.config.WarehouseContext;
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
 * Production implementation of {@link WarehouseService} for a single-warehouse system.
 *
 * <p>All mutating operations (update, activate, deactivate) are guarded by
 * {@link WarehouseContext} so the acting user always operates on their own
 * warehouse. The warehouse ID is never accepted as an external parameter.</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final EmailUtils           emailUtils;
    private final VerifiedProofRepository proofRepo;
    private final WarehouseContext warehouseContext;

    // ── Public onboarding ────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Single-warehouse guard: throws immediately if a warehouse already exists.
     * Validates OTP proof tokens, creates warehouse + admin atomically, then
     * deletes the tokens to prevent replay.</p>
     */
    @Override
    @Transactional
    public WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request) {

        // Single-warehouse enforcement
        if (warehouseRepository.count() > 0) {
            throw new AlreadyExistsException(
                    "A warehouse already exists. This system supports only one warehouse.");
        }

        // Validate email OTP proof
        VerifiedProof emailProof = proofRepo.findById(request.getEmailToken())
                .orElseThrow(() -> new BadRequestException("Email verification expired or invalid."));
        if (!emailProof.getIdentifier().equalsIgnoreCase(request.getAdminEmail())) {
            throw new BadRequestException("Email mismatch with verification token.");
        }

        // Validate contact OTP proof
        VerifiedProof contactProof = proofRepo.findById(request.getContactToken())
                .orElseThrow(() -> new BadRequestException("Contact verification expired or invalid."));
        if (!contactProof.getIdentifier().equals(request.getAdminContact())) {
            throw new BadRequestException("Contact mismatch with verification token.");
        }

        // Identity uniqueness checks
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

        // Create warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setActive(true);
        Warehouse saved = warehouseRepository.save(warehouse);

        // Create primary admin
        User admin = new User();
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setContactNumber(request.getAdminContact());
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setWarehouse(saved);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(admin);

        // Invalidate OTP tokens (one-time use)
        proofRepo.delete(emailProof);
        proofRepo.delete(contactProof);

        log.info("Warehouse '{}' created with Admin '{}'", saved.getName(), admin.getEmail());

        // Non-critical welcome email
        try {
            emailUtils.sendWarehouseWelcomeEmail(admin.getEmail(), admin.getName(), saved.getName());
        } catch (Exception e) {
            log.warn("Welcome email failed for {}: {}", admin.getEmail(), e.getMessage());
        }

        return mapToResponse(saved);
    }

    // ── Authenticated warehouse operations ───────────────────────────────────

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getCurrentWarehouse() {
        return mapToResponse(loadCurrentWarehouse());
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WarehouseResponse updateWarehouse(@Valid WarehouseRequest request) {
        Warehouse warehouse = loadCurrentWarehouse();
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        return mapToResponse(warehouseRepository.save(warehouse));
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void activateWarehouse() {
        Warehouse warehouse = loadCurrentWarehouse();
        warehouse.setActive(true);
        log.info("Warehouse {} activated", warehouse.getId());
    }

    /** {@inheritDoc} */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivateWarehouse() {
        Warehouse warehouse = loadCurrentWarehouse();
        warehouse.setActive(false);
        log.warn("Warehouse {} deactivated", warehouse.getId());
    }

    // ── Internal helpers used by scheduled jobs ──────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Used by scheduled tasks (e.g., expiry processor) that run outside an
     * HTTP request context and therefore have no JWT to extract a warehouse ID from.
     * Since this is a single-warehouse system there is exactly one active warehouse.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public String getSingleWarehouseId() {
        return warehouseRepository.findAll().stream()
                .filter(Warehouse::isActive)
                .map(Warehouse::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active warehouse found. Run /api/v1/warehouses/setup first."));
    }

    // ── Private utilities ────────────────────────────────────────────────────

    private Warehouse loadCurrentWarehouse() {
        String warehouseId = warehouseContext.getWarehouseId();
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
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
