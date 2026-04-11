package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.VerifiedProof;
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

    private final VerifiedProofRepository proofRepo;
    /**
     * {@inheritDoc}
     * <p>
     * <b>Updated Implementation:</b>
     * <ul>
     * <li><b>Proof Validation:</b> Verifies Redis tokens for both Email and Contact.</li>
     * <li><b>Identity Guard:</b> Ensures the verified identifiers match the form data.</li>
     * <li><b>Secure Passwords:</b> Uses the password provided by the user in the request.</li>
     * </ul>
     * Tokens are deleted immediately after successful persistence to prevent replay attacks.
     */

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request) {

        // 1. Validate and Fetch Email Proof from Redis
        VerifiedProof emailProof = proofRepo.findById(request.getEmailToken())
                .orElseThrow(() -> new BadRequestException("Email verification expired or invalid."));

        if (!emailProof.getIdentifier().equalsIgnoreCase(request.getAdminEmail())) {
            throw new BadRequestException("Security Alert: The verified email does not match the provided admin email.");
        }

        // 2. Validate and Fetch Contact Proof from Redis
        VerifiedProof contactProof = proofRepo.findById(request.getContactToken())
                .orElseThrow(() -> new BadRequestException("Contact verification expired or invalid."));

        if (!contactProof.getIdentifier().equals(request.getAdminContact())) {
            throw new BadRequestException("Security Alert: The verified contact does not match the provided admin contact.");
        }

        // 3. Uniqueness Checks
        // Business Rule: Duplicate names allowed only if locations are different
        if (warehouseRepository.existsByNameIgnoreCaseAndLocationIgnoreCase(request.getName(), request.getLocation())) {
            throw new AlreadyExistsException("A warehouse with this name already exists at this location.");
        }

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new AlreadyExistsException("The admin email '" + request.getAdminEmail() + "' is already in use.");
        }

        if(userRepository.existsByContactNumber(request.getAdminContact())){
            throw new AlreadyExistsException("The admin contact '"+ request.getAdminContact()+"' is already in use.");
        }

        // 4. Persist the Warehouse Facility
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setActive(true);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        // 5. Initialize the Primary Admin User
        User admin = new User();
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setContactNumber(request.getAdminContact());
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setWarehouse(savedWarehouse);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(admin);

        // 6. STRICT CLEANUP: Delete proof tokens from Redis immediately
        // This ensures the same tokens cannot be used to call this API again.
        proofRepo.delete(emailProof);
        proofRepo.delete(contactProof);

        log.info("Redis proof tokens consumed and deleted for: {}", admin.getEmail());

        // 7. Asynchronous Welcome Notification
        try {
            emailUtils.sendWarehouseWelcomeEmail(
                    admin.getEmail(),
                    admin.getName(),
                    savedWarehouse.getName()
            );
        } catch (Exception e) {
            log.warn("Warehouse '{}' created, but welcome email failed for {}: {}",
                    savedWarehouse.getName(), admin.getEmail(), e.getMessage());
        }

        log.info("SUCCESS: Verified Facility '{}' (ID: {}) initialized for Admin '{}'.",
                savedWarehouse.getName(), savedWarehouse.getId(), admin.getEmail());

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