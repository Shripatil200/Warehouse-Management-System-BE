package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Identity Management and Staff Orchestration.
 * <p>
 * This service manages the staff lifecycle within strict multi-tenant boundaries.
 * It enforces a "Silo" architecture where users are isolated by their assigned warehouse facility.
 * </p>
 */
public interface UserService {

    /**
     * Unified Advanced Search API for dynamic staff filtering.
     * <p>
     * <b>Capabilities:</b>
     * <ul>
     * <li><b>Fuzzy Search:</b> Matches 'query' against name or email.</li>
     * <li><b>Dynamic Filtering:</b> Combine role, status, and search query in one call.</li>
     * <li><b>Silo Enforcement:</b> Automatically scopes results to the requester's warehouse.</li>
     * <li><b>Performance:</b> Returns a paginated result to ensure high performance.</li>
     * </ul>
     * </p>
     * @param query    Optional string for name/email search.
     * @param role     Optional {@link Role} filter.
     * @param status   Optional {@link UserStatus} filter.
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Page} of matching user profiles.
     */
    Page<UserResponse> searchUsers(String query, Role role, UserStatus status, Pageable pageable);

    /**
     * Provisions a new staff member and triggers onboarding.
     * <p>
     * <b>Onboarding Logic:</b>
     * <ul>
     * <li><b>Hierarchy Check:</b> Managers can only create {@link Role#EMPLOYEE} accounts.</li>
     * <li><b>Security Boundary:</b> Binds the user to the requester's warehouse facility.</li>
     * <li><b>Credentialing:</b> Generates a deterministic temporary password.</li>
     * </ul>
     * </p>
     * @param request User profile and facility assignment details.
     * @return A success message confirming account creation.
     */
    String createUser(@Valid UserRequest request);

    /**
     * Updates profile metadata or modifies authorization levels (Roles).
     * <p>
     * <b>Validation:</b> Ensures email/contact uniqueness and restricts
     * role promotions strictly to the {@link Role#ADMIN}.
     * </p>
     * @param id Target User UUID.
     * @param request The update payload.
     * @return A confirmation message.
     */
    String updateUserDetails(String id, UserUpdate request);

    /**
     * Manages the operational availability of a staff member.
     * <p>
     * <b>Safety Constraints:</b> Prevents self-lockout and restricts Managers
     * from modifying statuses of equivalent or higher-ranking roles.
     * </p>
     * @param id Target User UUID.
     * @param status New status (ACTIVE, INACTIVE, etc.).
     */
    void updateStatus(String id, UserStatus status);

    /**
     * Performs a logical 'Soft-Delete' of a user account.
     * <p>
     * <b>Data Integrity:</b> Marks the record as DELETED to maintain
     * referential integrity for historical audit trails.
     * </p>
     * @param id User UUID to be deactivated.
     */
    void deleteUser(String id);

    /**
     * Fetches the detailed profile information for a specific staff member.
     * @param id User UUID.
     * @return User details mapped to a response DTO.
     */
    UserResponse getUserById(String id);

    /**
     * Retrieves the profile of the currently authenticated user.
     * @return The profile of the session holder.
     */
    UserResponse getMyProfile();

    /**
     * Retrieves a paginated list of all non-deleted staff members within
     * the requester's warehouse.
     * * @param pageable Pagination metadata.
     * @return A {@link Page} of user profiles.
     */
    Page<UserResponse> getAllUser(Pageable pageable);
}