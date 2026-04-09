package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.UserService;
import com.infotact.warehouse.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link UserService} for Identity and Access Management.
 * <p>
 * This service manages the staff lifecycle within the boundaries of a specific facility.
 * It implements strict hierarchical security where <code>ADMIN</code> users hold
 * promotion/deletion authority, while <code>MANAGER</code> users handle daily status
 * and profile management.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtils emailUtils;

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    /**
     * Internal helper to verify if the requester has specific authority.
     */
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(target));
    }

    /**
     * Resolves the session holder's profile.
     * <p><b>Optimization:</b> Cached by username to reduce repeated SecurityContext
     * lookups during multi-tenant validation.</p>
     */
    @Cacheable(value = "userProfiles", key = "'auth_' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    /**
     * Enforces the 'Silo' boundary.
     * Ensures the current user and the target user belong to the same facility.
     */
    private void validateWarehouseAccess(User currentUser, User targetUser) {
        if (currentUser.getWarehouse() == null || targetUser.getWarehouse() == null ||
                !currentUser.getWarehouse().getId().equals(targetUser.getWarehouse().getId())) {
            log.error("Multi-tenancy Violation: User {} tried to access ID {}", currentUser.getEmail(), targetUser.getId());
            throw new UnauthorizedException("Access Denied: You cannot manage users outside your warehouse context.");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Enforces deterministic password policy: <code>Welcome@ + [Last 4 phone digits]</code>.</li>
     * <li>Strictly prevents Managers from creating Admin-level accounts.</li>
     * <li>Triggers an onboarding email containing temporary credentials.</li>
     * </ul>
     */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    public String createUser(UserRequest request) {
        User currentUser = getAuthenticatedUser();
        Role targetRole = Role.valueOf(request.getRole().toUpperCase());

        if (targetRole == Role.ADMIN && !hasRole(ROLE_ADMIN)) {
            throw new UnauthorizedException("Insufficient Privilege: Only Admins can provision other Admin accounts.");
        }

        if (!currentUser.getWarehouse().getId().equals(request.getWarehouseId())) {
            throw new UnauthorizedException("Multi-tenancy Error: Warehouse assignment mismatch.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new AlreadyExistsException("A user with this email is already registered.");

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setContactNumber(request.getContactNumber());
        newUser.setRole(targetRole);
        newUser.setStatus(UserStatus.INACTIVE);
        newUser.setWarehouse(warehouse);

        String phone = request.getContactNumber();
        String tempPassword = "Welcome@" + (phone.length() >= 4 ? phone.substring(phone.length() - 4) : "1234");
        newUser.setPassword(passwordEncoder.encode(tempPassword));

        userRepository.save(newUser);
        emailUtils.passwordUpdatedEmail(newUser.getEmail(), "Account Provisioned", tempPassword);

        return "User created successfully with role " + targetRole;
    }

    /**
     * {@inheritDoc}
     * <p><b>Filtering:</b> Strictly limited to the authenticated user's warehouse.</p>
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Cacheable(value = "warehouseUsers", key = "'list_' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public List<UserResponse> getAllUser() {
        User currentUser = getAuthenticatedUser();
        return userRepository.findAllByWarehouse(currentUser.getWarehouse().getId())
                .stream().map(UserResponse::new).toList();
    }

    /**
     * {@inheritDoc}
     * <p><b>RBAC Logic:</b> Profile metadata updates are permitted for Managers,
     * but Role modifications (Promotions) are strictly reserved for Admins.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    public String updateUserDetails(String id, UserUpdate request) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        validateWarehouseAccess(currentUser, targetUser);

        if (request.getName() != null) targetUser.setName(request.getName());
        if (request.getEmail() != null) targetUser.setEmail(request.getEmail());
        if (request.getContactNumber() != null) targetUser.setContactNumber(request.getContactNumber());

        if (request.getRole() != null) {
            if (!hasRole(ROLE_ADMIN)) {
                log.warn("Unauthorized Role Update: Manager {} attempted to promote {}", currentUser.getEmail(), targetUser.getEmail());
                throw new UnauthorizedException("Access Denied: Role modifications require Admin clearance.");
            }
            targetUser.setRole(Role.valueOf(request.getRole().toUpperCase()));
        }

        userRepository.save(targetUser);
        return "User profile updated successfully.";
    }

    /**
     * {@inheritDoc}
     * <p><b>Soft-Delete:</b> Sets status to <code>DELETED</code> to maintain
     * referential integrity for historical order logs.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    public void deleteUser(String id) {
        if (!hasRole(ROLE_ADMIN)) throw new UnauthorizedException("Only Admins can deactivate user accounts.");

        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        User currentUser = getAuthenticatedUser();

        validateWarehouseAccess(currentUser, targetUser);
        targetUser.setStatus(UserStatus.DELETED);
        userRepository.save(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userProfiles", key = "#id")
    public UserResponse getUserById(String id) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        validateWarehouseAccess(currentUser, targetUser);
        return new UserResponse(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void updateStatus(String userId, UserStatus status) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateWarehouseAccess(currentUser, targetUser);
        targetUser.setStatus(status);
        userRepository.save(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        User currentUser = getAuthenticatedUser();
        return userRepository.findActiveByWarehouse(currentUser.getWarehouse().getId())
                .stream().map(UserResponse::new).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<UserResponse> getUsersByRole(Role role) {
        User currentUser = getAuthenticatedUser();
        return userRepository.findByWarehouseAndRole(currentUser.getWarehouse().getId(), role)
                .stream().map(UserResponse::new).toList();
    }
}