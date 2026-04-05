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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserService}.
 * Enforces Role-Based Access Control (RBAC) and data isolation between warehouses.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtils emailUtils;

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    /**
     * Internal utility to check the roles of the currently authenticated user.
     */
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(target));
    }

    /**
     * Retrieves the User entity of the person making the request.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    /**
     * Logic for Multi-Tenant Isolation.
     * Ensures that Managers/Admins cannot access or modify users from a different warehouse.
     */
    private void validateWarehouseAccess(User currentUser, User targetUser) {
        if (hasRole(ROLE_SUPER_ADMIN)) return;

        if (currentUser.getWarehouse() == null || targetUser.getWarehouse() == null ||
                !currentUser.getWarehouse().getId().equals(targetUser.getWarehouse().getId())) {
            throw new UnauthorizedException("Access Denied: You cannot manage users in other warehouses.");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String createUser(UserRequest request) {
        User currentUser = getAuthenticatedUser();
        Role targetRole = Role.valueOf(request.getRole().toUpperCase());

        // Hierarchical Security: Admin creation is global (Super Admin only)
        if (targetRole == Role.ADMIN && !hasRole(ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("Only Super Admins can create Admin accounts.");
        }

        // Warehouse Isolation: Prevent cross-warehouse user creation
        if (!hasRole(ROLE_SUPER_ADMIN)) {
            if (!currentUser.getWarehouse().getId().equals(request.getWarehouseId())) {
                throw new UnauthorizedException("You can only create users for your own warehouse.");
            }

            if (targetRole == Role.EMPLOYEE && !hasRole(ROLE_ADMIN) && !hasRole(ROLE_MANAGER)) {
                throw new UnauthorizedException("Insufficient permissions to create an employee.");
            }
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new AlreadyExistsException("Email already registered.");

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setContactNumber(request.getContactNumber());
        newUser.setRole(targetRole);
        newUser.setStatus(UserStatus.INACTIVE);
        newUser.setWarehouse(warehouse);

        // Auto-generate password: Welcome@{Last4DigitsOfPhone}
        String phone = request.getContactNumber();
        String lastFour = phone.length() >= 4 ? phone.substring(phone.length() - 4) : "1234";
        String tempPassword = "Welcome@" + lastFour;
        newUser.setPassword(passwordEncoder.encode(tempPassword));

        userRepository.save(newUser);
        emailUtils.passwordUpdatedEmail(newUser.getEmail(), "Account Created", tempPassword);

        return "User created successfully with role " + targetRole + " in warehouse " + warehouse.getName();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUser() {
        if (hasRole(ROLE_SUPER_ADMIN)) {
            return userRepository.findByRole(Role.ADMIN)
                    .stream().map(UserResponse::new).collect(Collectors.toList());
        }

        User currentUser = getAuthenticatedUser();
        return userRepository.findAllByWarehouse(currentUser.getWarehouse().getId())
                .stream().map(UserResponse::new).collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String updateUserDetails(String id, UserUpdate request) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (targetUser.getRole() == Role.ADMIN && !hasRole(ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("Only Super Admins can modify Admin accounts.");
        }

        validateWarehouseAccess(currentUser, targetUser);

        if (request.getName() != null) targetUser.setName(request.getName());

        // Role Promotion Constraints
        if (request.getRole() != null) {
            Role newRole = Role.valueOf(request.getRole().toUpperCase());

            if (hasRole(ROLE_ADMIN)) {
                if (newRole == Role.ADMIN || newRole == Role.SUPER_ADMIN) {
                    throw new UnauthorizedException("Admins cannot promote users to Admin roles.");
                }
                targetUser.setRole(newRole);
            } else if (hasRole(ROLE_SUPER_ADMIN)) {
                targetUser.setRole(newRole);
            } else {
                throw new UnauthorizedException("Managers cannot change user roles.");
            }
        }

        userRepository.save(targetUser);
        return "User updated successfully.";
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteUser(String id) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (targetUser.getRole() == Role.ADMIN && !hasRole(ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("Only Super Admins can delete Admin accounts.");
        }

        if (!hasRole(ROLE_ADMIN) && !hasRole(ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("Only Admins can delete users.");
        }

        User currentUser = getAuthenticatedUser();
        validateWarehouseAccess(currentUser, targetUser);

        targetUser.setStatus(UserStatus.DELETED);
        userRepository.save(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void updateStatus(String userId, UserStatus status) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateWarehouseAccess(currentUser, targetUser);

        if (!hasRole(ROLE_ADMIN) && !hasRole(ROLE_MANAGER) && !hasRole(ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("Access denied.");
        }

        targetUser.setStatus(status);
        userRepository.save(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        validateWarehouseAccess(currentUser, targetUser);
        return new UserResponse(targetUser);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        User currentUser = getAuthenticatedUser();
        return userRepository.findActiveByWarehouse(currentUser.getWarehouse().getId())
                .stream().map(UserResponse::new).collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(Role role) {
        User currentUser = getAuthenticatedUser();
        return userRepository.findByWarehouseAndRole(currentUser.getWarehouse().getId(), role)
                .stream().map(UserResponse::new).collect(Collectors.toList());
    }
}