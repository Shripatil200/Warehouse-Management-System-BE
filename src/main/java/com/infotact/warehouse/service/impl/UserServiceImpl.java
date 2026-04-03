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
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtils emailUtils;

    /**
     * ROLE CHECK HELPER
     */
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(target));
    }

    /**
     * AUTHENTICATION HELPER
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    /**
     * WAREHOUSE ISOLATION HELPER
     */
    private void validateWarehouseAccess(User currentUser, User targetUser) {
        if (!currentUser.getWarehouse().getId().equals(targetUser.getWarehouse().getId())) {
            throw new UnauthorizedException("Access Denied: You cannot manage users in other warehouses.");
        }
    }

    @Override
    @Transactional
    public String createUser(UserRequest request) {
        // 1. Authorization Check
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Only Admins or Managers can create users.");
        }

        User currentUser = getAuthenticatedUser();
        if (!request.getWarehouseId().equals(currentUser.getWarehouse().getId())) {
            throw new UnauthorizedException("You can only onboard employees for your own warehouse.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new AlreadyExistsException("Email already registered.");

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setContactNumber(request.getContactNumber());
        newUser.setRole(Role.EMPLOYEE);
        newUser.setStatus(UserStatus.INACTIVE);
        newUser.setWarehouse(warehouse);

        String phone = request.getContactNumber();
        String lastFour = phone.substring(Math.max(0, phone.length() - 4));
        newUser.setPassword(passwordEncoder.encode("Welcome@" + lastFour));

        userRepository.save(newUser);
        emailUtils.passwordUpdatedEmail(newUser.getEmail(), "Account Created", "Welcome@" + lastFour);

        return "User created successfully in " + warehouse.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUser() {
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        return userRepository.findAllByWarehouse(currentUser.getWarehouse().getId())
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateStatus(String userId, UserStatus status) {
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateWarehouseAccess(currentUser, targetUser);

        targetUser.setStatus(status);
        userRepository.save(targetUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        validateWarehouseAccess(currentUser, targetUser);
        return new UserResponse(targetUser);
    }

    @Override
    @Transactional
    public String updateUserDetails(String id, UserUpdate request) {
        if (!hasRole("ADMIN")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        validateWarehouseAccess(currentUser, targetUser);

        if (request.getName() != null) targetUser.setName(request.getName());

        // Explicit role change authorization
        if (request.getRole() != null) {
            if (hasRole("ADMIN")) {
                targetUser.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } else {
                throw new UnauthorizedException("Managers cannot change user roles.");
            }
        }

        userRepository.save(targetUser);
        return "User updated successfully.";
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        if (!hasRole("ADMIN")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        validateWarehouseAccess(currentUser, targetUser);

        targetUser.setStatus(UserStatus.DELETED);
        userRepository.save(targetUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllActiveUsers() {
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        return userRepository.findActiveByWarehouse(currentUser.getWarehouse().getId())
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(Role role) {
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Access denied.");
        }

        User currentUser = getAuthenticatedUser();
        return userRepository.findByWarehouseAndRole(currentUser.getWarehouse().getId(), role)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }
}