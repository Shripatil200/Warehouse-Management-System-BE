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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link UserService} for Identity and Access Management (IAM).
 * <p>
 * This service manages the staff lifecycle within strict multi-tenant boundaries.
 * It enforces a "Silo" architecture where users are isolated by their assigned warehouse facility.
 * </p>
 * <p><b>Security Principles:</b>
 * <ul>
 * <li><b>Hierarchy:</b> Managers manage Employees; only Admins manage Managers/Admins.</li>
 * <li><b>Isolation:</b> Cross-warehouse data access is strictly prohibited.</li>
 * <li><b>Integrity:</b> Soft-delete is used to preserve historical transaction audits.</li>
 * </ul>
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
     * Internal helper to verify the security clearance of the current session holder.
     * @param role The role string to check (e.g., "ROLE_ADMIN").
     * @return true if the user holds the specified authority.
     */
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(target));
    }

    /**
     * Resolves the profile of the currently authenticated user from the SecurityContext.
     * <p><b>Cache Optimization:</b> Uses Spring Cache to reduce database hits for
     * multi-tenant validation on every request.</p>
     * @return The {@link User} entity of the current requester.
     */
    @Cacheable(value = "userProfiles", key = "'auth_' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    /**
     * Enforces the multi-tenant "Silo" boundary.
     * @throws UnauthorizedException if the requester attempts to access data outside their assigned warehouse.
     */
    private void validateWarehouseAccess(User currentUser, User targetUser) {
        if (currentUser.getWarehouse() == null || targetUser.getWarehouse() == null ||
                !currentUser.getWarehouse().getId().equals(targetUser.getWarehouse().getId())) {
            log.error("Multi-tenancy Violation: User {} tried to access ID {}", currentUser.getEmail(), targetUser.getId());
            throw new UnauthorizedException("Access Denied: You cannot manage users outside your warehouse context.");
        }
    }

    /**
     * Enforces the hierarchical management rule.
     * <p>Prevents Managers from modifying other Managers or Admins. Only Admins have authority
     * over management-level personnel.</p>
     */
    private void validateHierarchy(User targetUser) {
        if (hasRole(ROLE_MANAGER) && !hasRole(ROLE_ADMIN)) {
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.MANAGER) {
                throw new UnauthorizedException("Hierarchy Violation: Managers can only manage Staff/Employees.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p><b>Business Logic:</b>
     * <ul>
     * <li>Generates a temporary password: Welcome@ + [Last 4 digits of phone].</li>
     * <li>Assigns the user to the requester's warehouse facility.</li>
     * <li>Triggers onboarding email with credentials.</li>
     * </ul>
     */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    public String createUser(UserRequest request) {
        User currentUser = getAuthenticatedUser();
        Role targetRole = Role.valueOf(request.getRole().toUpperCase());

        if (targetRole != Role.EMPLOYEE && !hasRole(ROLE_ADMIN)) {
            throw new UnauthorizedException("Insufficient Privilege: Only Admins can provision non-employee accounts.");
        }

        if (!currentUser.getWarehouse().getId().equals(request.getWarehouseId())) {
            throw new UnauthorizedException("Multi-tenancy Error: You cannot create users for a different facility.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new AlreadyExistsException("A user with this email is already registered.");

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse facility not found."));

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
     * <p>Returns a paginated view of staff members restricted to the requester's warehouse.</p>
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<UserResponse> getAllUser(Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        return userRepository.findAllByWarehouse(currentUser.getWarehouse().getId(), pageable)
                .map(UserResponse::new);
    }

    /**
     * {@inheritDoc}
     * <p><b>Validation Logic:</b>
     * <ul>
     * <li>Self-update is permitted for all users.</li>
     * <li>Email and Contact uniqueness is verified against other existing users.</li>
     * <li>Role promotion/demotion is strictly reserved for ADMIN role.</li>
     * </ul>
     */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    public String updateUserDetails(String id, UserUpdate request) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!currentUser.getId().equals(targetUser.getId())) {
            validateWarehouseAccess(currentUser, targetUser);
            validateHierarchy(targetUser);
        }

        if (request.getEmail() != null && !targetUser.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AlreadyExistsException("The email '" + request.getEmail() + "' is already in use by another account.");
            }
            targetUser.setEmail(request.getEmail());
        }

        if (request.getContactNumber() != null && !targetUser.getContactNumber().equalsIgnoreCase(request.getContactNumber())) {
            if (userRepository.existsByContactNumber(request.getContactNumber())) {
                throw new AlreadyExistsException("The contact number '" + request.getContactNumber() + "' is already registered.");
            }
            targetUser.setContactNumber(request.getContactNumber());
        }

        if (request.getName() != null) targetUser.setName(request.getName());

        if (request.getRole() != null) {
            if (!hasRole(ROLE_ADMIN)) {
                throw new UnauthorizedException("Access Denied: Role modifications require Admin clearance.");
            }
            targetRoleModification(targetUser, request.getRole());
        }

        userRepository.save(targetUser);
        return "User profile updated successfully.";
    }

    private void targetRoleModification(User user, String role) {
        user.setRole(Role.valueOf(role.toUpperCase()));
    }

    /**
     * {@inheritDoc}
     * <p>Sets user status to <code>DELETED</code>. Prevents self-deletion.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = {"userProfiles", "warehouseUsers"}, allEntries = true)
    public void deleteUser(String id) {
        if (!hasRole(ROLE_ADMIN)) throw new UnauthorizedException("Only Admins can perform account deactivation.");

        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        User currentUser = getAuthenticatedUser();

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("Safety Violation: You cannot delete your own account.");
        }

        validateWarehouseAccess(currentUser, targetUser);
        targetUser.setStatus(UserStatus.DELETED);
        userRepository.save(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!currentUser.getId().equals(targetUser.getId())) {
            validateWarehouseAccess(currentUser, targetUser);
        }

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

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("Self-Modification Error: You cannot change your own operational status.");
        }

        validateWarehouseAccess(currentUser, targetUser);
        validateHierarchy(targetUser);

        targetUser.setStatus(status);
        userRepository.save(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return new UserResponse(getAuthenticatedUser());
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