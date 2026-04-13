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

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link UserService} for Identity and Access Management (IAM).
 * <p>
 * This service manages the staff lifecycle within strict multi-tenant boundaries.
 * It enforces a "Silo" architecture where users are isolated by their assigned warehouse facility
 * and administrative actions are governed by a hierarchical Role-Based Access Control (RBAC) model.
 * </p>
 *
 * @author Gemini
 * @version 1.2
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

    // --- SEARCH & RETRIEVAL LOGIC ---

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Note:</b> This method utilizes the JPA Criteria API through {@link Specification}
     * to dynamically generate the SQL {@code WHERE} clause. It automatically injects the
     * current user's warehouse ID to prevent cross-tenant data leakage.
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

            // 1. Status Filtering (STRICT)

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // default behavior → hide deleted
                predicates.add(cb.notEqual(root.get("status"), UserStatus.DELETED));
            }

            // 3. Dynamic Fuzzy Search (Name or Email)
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("contactNumber")), pattern),
                        cb.like(cb.lower(root.get("id")),pattern)
                ));
            }

            // 4. Exact Match Role Filter
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            // 5. Exact Match Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
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
        return userRepository.findAllByWarehouse(currentUser.getWarehouse().getId(), pageable).map(UserResponse::new);
    }

    /**
     * {@inheritDoc}
     * <p><b>RBAC:</b> Users can retrieve themselves; Managers/Admins can retrieve anyone in their warehouse silo.</p>
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
     * <b>Implementation Note:</b> Password generation uses the {@code phone.substring} strategy
     * to provide a deterministic temp password. Triggers asynchronous email dispatch.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", allEntries = true)
    public String createUser(UserRequest request) {
        User currentUser = getAuthenticatedUser();
        Role targetRole = Role.valueOf(request.getRole().toUpperCase());

        // 1. Hierarchy Check
        if (targetRole != Role.EMPLOYEE && !hasRole(ROLE_ADMIN)) {
            throw new UnauthorizedException("Insufficient Privilege: Only Admins can provision management accounts.");
        }

        // 2. Silo Check
        if (!currentUser.getWarehouse().getId().equals(request.getWarehouseId())) {
            throw new UnauthorizedException("Multi-tenancy Error: Warehouse assignment mismatch.");
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
        newUser.setStatus(UserStatus.PENDING);
        newUser.setWarehouse(warehouse);

        String phone = request.getContactNumber();
        String tempPassword = "Welcome@" + (phone.length() >= 4 ? phone.substring(phone.length() - 4) : "1234");
        newUser.setPassword(passwordEncoder.encode(tempPassword));

        userRepository.save(newUser);
        emailUtils.passwordUpdatedEmail(newUser.getEmail(), "Account Provisioned", tempPassword);

        return "User created successfully.";
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Note:</b> Unique constraint fields (Email/Phone) are validated
     * before persistence to prevent {@code DataIntegrityViolationException}.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", allEntries = true)
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
            if (!hasRole(ROLE_ADMIN)) throw new UnauthorizedException("Role modifications require Admin clearance.");
            targetUser.setRole(Role.valueOf(request.getRole().toUpperCase()));
        }

        userRepository.save(targetUser);
        return "User profile updated successfully.";
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Soft-Delete Strategy:</b> Sets status to {@code DELETED} and appends a timestamp
     * to unique identifiers (Email/Contact) to allow future re-registration of the same values.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", allEntries = true)
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
    @CacheEvict(value = "userProfiles", allEntries = true)
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

    // --- CACHING & AUTHENTICATION HELPERS ---

    /**
     * Retrieves the {@link User} entity for the current session.
     * @return The authenticated user entity.
     * @throws UnauthorizedException if no profile is found in the repository.
     */
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getCachedUser(auth.getName());
    }

    /**
     * Helper to resolve user profiles with caching support.
     * @param email Primary key (email) for lookup.
     * @return Cached or fresh User entity.
     */
    @Cacheable(value = "userProfiles", key = "#email")
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
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.MANAGER) {
                throw new UnauthorizedException("Hierarchy Violation: Managers can only manage Employees.");
            }
        }
    }

    private void validateWarehouseAccess(User currentUser, User targetUser) {
        if (!currentUser.getWarehouse().getId().equals(targetUser.getWarehouse().getId())) {
            throw new UnauthorizedException("Access Denied: Silo boundary violation.");
        }
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(target));
    }
}