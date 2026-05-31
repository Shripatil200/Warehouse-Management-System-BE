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
import com.infotact.warehouse.service.TokenBlacklistService;
import com.infotact.warehouse.service.UserService;
import com.infotact.warehouse.config.JWT.JwtUtil;
import com.infotact.warehouse.util.EmailUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link UserService} for Identity and Access Management (IAM).
 * <p>
 * This service manages the staff lifecycle within the single warehouse.
 * It ensures users are always scoped to the warehouse they were created in
 * and administrative actions are governed by a hierarchical Role-Based Access Control (RBAC) model.
 * </p>
 *
 * @author Gemini
 * @version 1.3
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtils emailUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest httpRequest;

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TEMP_PW_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#";
    private static final int TEMP_PW_LENGTH = 12;

    /** Generates a cryptographically random temporary password. */
    private static String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PW_LENGTH);
        for (int i = 0; i < TEMP_PW_LENGTH; i++) {
            sb.append(TEMP_PW_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
    }

    // --- SEARCH & RETRIEVAL LOGIC ---

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Note:</b> Utilizes JPA Criteria API. Injects current user's warehouse ID.
     * Restricted to MANAGER and ADMIN roles only.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<UserResponse> searchUsers(String query, Role role, UserStatus status, Pageable pageable) {
        User currentUser = getAuthenticatedUser();

        Specification<User> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Silo Filter
            predicates.add(cb.equal(root.get("warehouse").get("id"), currentUser.getWarehouse().getId()));

            // 2. Status Filtering
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), UserStatus.DELETED));
            }

            // 3. Dynamic Fuzzy Search
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("contactNumber")), pattern),
                        cb.like(cb.lower(root.get("id")), pattern)
                ));
            }

            // 4. Exact Match Role Filter
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(UserResponse::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<UserResponse> getAllUser(Pageable pageable) {
        User currentUser = getAuthenticatedUser();
        return userRepository.findAllByWarehouse(currentUser.getWarehouse().getId(), UserStatus.DELETED, pageable).map(UserResponse::new);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return new UserResponse(getAuthenticatedUser());
    }

    // --- LIFECYCLE OPERATIONS ---

    /**
     * {@inheritDoc}
     * <p>
     * <b>RBAC:</b> Managers can create OPERATOR. ADMIN can create any role.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfilesV2", allEntries = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String createUser(UserRequest request) {
        User currentUser = getAuthenticatedUser();
        Role targetRole = Role.valueOf(request.getRole().toUpperCase());

        // Hierarchy Check
        boolean isStaffRole = (targetRole == Role.OPERATOR);
        if (!isStaffRole && !hasRole(ROLE_ADMIN)) {
            throw new UnauthorizedException("Insufficient Privilege: Only Admins can provision management accounts.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new AlreadyExistsException("Email already registered.");

        // In a single-warehouse system, the warehouse is always the admin's warehouse.
        Warehouse warehouse = currentUser.getWarehouse();

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setContactNumber(request.getContactNumber());
        newUser.setRole(targetRole);
        newUser.setStatus(UserStatus.PENDING);
        newUser.setWarehouse(warehouse);
        if (targetRole == Role.OPERATOR) {
            newUser.setSpecialty(request.getSpecialty());
        }

        String tempPassword = generateTempPassword();
        newUser.setPassword(passwordEncoder.encode(tempPassword));

        userRepository.save(newUser);
        emailUtils.sendTeamMemberWelcomeEmail(newUser.getEmail(), newUser.getName(), targetRole.name(), tempPassword);

        return "User created successfully.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfilesV2", allEntries = true)
    public String updateUserDetails(String id, UserUpdate request) {
        User currentUser = getAuthenticatedUser();
        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!currentUser.getId().equals(targetUser.getId())) {
            validateWarehouseAccess(currentUser, targetUser);
            validateHierarchy(targetUser);
        }

        validateUniqueness(targetUser, request);

        if (request.getName() != null) targetUser.setName(request.getName());
        if (request.getEmail() != null) targetUser.setEmail(request.getEmail());
        if (request.getContactNumber() != null) targetUser.setContactNumber(request.getContactNumber());

        if (request.getRole() != null) {
            Role newRole = Role.valueOf(request.getRole().toUpperCase());
            if (targetUser.getRole() != newRole) {
                if (!hasRole(ROLE_ADMIN)) throw new UnauthorizedException("Role modifications require Admin clearance.");
                targetUser.setRole(newRole);
                if (newRole != Role.OPERATOR) {
                    targetUser.setSpecialty(null);
                }
                // Force re-login of the target user if they are modifying themselves
                if (currentUser.getId().equals(targetUser.getId())) {
                    blacklistCurrentToken();
                }
            }
        }

        if (targetUser.getRole() == Role.OPERATOR) {
            targetUser.setSpecialty(request.getSpecialty());
        } else {
            targetUser.setSpecialty(null);
        }

        userRepository.save(targetUser);
        return "User profile updated successfully.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfilesV2", allEntries = true)
    public void deleteUser(String id) {
        if (!hasRole(ROLE_ADMIN)) throw new UnauthorizedException("Only Admins can deactivate accounts.");

        User targetUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        User currentUser = getAuthenticatedUser();

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("Safety Violation: You cannot delete your own account.");
        }

        validateWarehouseAccess(currentUser, targetUser);

        targetUser.setStatus(UserStatus.DELETED);
        String suffix = "-DEL-" + System.currentTimeMillis();
        targetUser.setEmail(targetUser.getEmail() + suffix);
        targetUser.setContactNumber(targetUser.getContactNumber() + suffix);

        userRepository.save(targetUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfilesV2", allEntries = true)
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
        // If deactivating or suspending, the target user's token should no longer be accepted.
        // We can't reach into their session, but we log a warning for audit purposes.
        // Full server-side revocation of another user's token requires storing the user's
        // current token at login time (not currently done). This is a known limitation.
        if (status == UserStatus.INACTIVE || status == UserStatus.DELETED) {
            log.warn("User {} status set to {} by admin — existing tokens will expire naturally " +
                            "within 10h. Consider adding per-user token versioning for immediate revocation.",
                    targetUser.getEmail(), status);
        }
    }

    // --- CACHING & AUTHENTICATION HELPERS ---

    @Override
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCachedUser(auth.getName());
    }

    @Cacheable(value = "userProfilesV2", key = "#email")
    public User getCachedUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException("User profile not found."));
    }

    // --- PRIVATE VALIDATION HELPERS ---

    private void validateUniqueness(User targetUser, UserUpdate request) {
        if (request.getEmail() != null && !targetUser.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) throw new AlreadyExistsException("Email already taken.");
        }
        if (request.getContactNumber() != null && !targetUser.getContactNumber().equalsIgnoreCase(request.getContactNumber())) {
            if (userRepository.existsByContactNumber(request.getContactNumber())) throw new AlreadyExistsException("Contact already taken.");
        }
    }

    private void validateHierarchy(User targetUser) {
        if (hasRole(ROLE_MANAGER) && !hasRole(ROLE_ADMIN)) {
            // Updated Logic: Managers can manage OPERATOR roles.
            // Managers are blocked from modifying ADMIN or other MANAGER accounts.
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.MANAGER) {
                throw new UnauthorizedException("Hierarchy Violation: Managers can only manage Operators.");
            }
        }
    }

    private void validateWarehouseAccess(User currentUser, User targetUser) {
        if (!currentUser.getWarehouse().getId().equals(targetUser.getWarehouse().getId())) {
            throw new UnauthorizedException("Access Denied: You may only manage users within your own warehouse.");
        }
    }

    /** Blacklists the JWT that authorised the current HTTP request. */
    private void blacklistCurrentToken() {
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                long expiryMs = jwtUtil.extractExpirationMs(token);
                tokenBlacklistService.blacklist(token, expiryMs);
                log.info("Token blacklisted after sensitive account change.");
            } catch (Exception e) {
                log.warn("Could not blacklist token after account change: {}", e.getMessage());
            }
        }
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(target));
    }
}