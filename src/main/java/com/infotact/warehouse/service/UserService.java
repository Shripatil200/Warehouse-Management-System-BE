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
 * This service manages the staff lifecycle, role-based access control (RBAC),
 * and multi-tenant security boundaries. It ensures that staff assignments
 * and administrative actions remain strictly within the authorized facility scope (Silo Pattern).
 * </p>
 */
public interface UserService {

    /**
     * Provisions a new staff member and triggers onboarding.
     * <p>
     * <b>Onboarding Logic:</b>
     * <ul>
     * <li><b>Password Generation:</b> Creates a temporary, deterministic password
     * (e.g., Welcome@ + last 4 digits of phone).</li>
     * <li><b>Hierarchy Check:</b> Managers are restricted to creating {@link Role#EMPLOYEE}
     * accounts; only Admins can provision Manager/Admin roles.</li>
     * <li><b>Security Boundary:</b> Binds the user to a specific facility to ensure data isolation.</li>
     * <li><b>Notification:</b> Triggers an automated email containing onboarding credentials.</li>
     * </ul>
     * </p>
     * @param request User profile and facility assignment details.
     * @return A success message confirming account creation.
     * @throws com.infotact.warehouse.exception.UnauthorizedException if hierarchy or warehouse mismatch occurs.
     */
    String createUser(@Valid UserRequest request);

    /**
     * Retrieves a paginated list of staff members within the requester's warehouse.
     * <p>
     * <b>Visibility Rules:</b>
     * Requester is restricted to viewing users belonging to their assigned warehouse.
     * Paginated response ensures system performance for large-scale facilities.
     * </p>
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Page} of user profiles.
     */
    Page<UserResponse> getAllUser(Pageable pageable);

    /**
     * Manages the operational availability of a staff member.
     * <p>
     * <b>Safety Constraints:</b>
     * <ul>
     * <li><b>Self-Lockout Prevention:</b> Prevents the current user from deactivating themselves.</li>
     * <li><b>Hierarchy Protection:</b> Managers cannot change the status of other Managers or Admins.</li>
     * </ul>
     * </p>
     * @param id Target User UUID.
     * @param status New status (ACTIVE, INACTIVE, etc.).
     */
    void updateStatus(String id, UserStatus status);

    /**
     * Updates profile metadata or modifies authorization levels (Roles).
     * <p>
     * <b>Logic:</b>
     * <ul>
     * <li><b>Identity Validation:</b> Validates that the email/contact update is not already taken by another user.</li>
     * <li><b>Role Promotion:</b> Role modifications (e.g., promoting to Manager) are strictly reserved for {@link Role#ADMIN}.</li>
     * <li><b>Self-Update:</b> Users are permitted to update their own profile details regardless of management role.</li>
     * </ul>
     * </p>
     * @param id Target User UUID.
     * @param request The update payload.
     * @return A confirmation message.
     */
    String updateUserDetails(String id, UserUpdate request);

    /**
     * Fetches the detailed profile information for a specific staff member.
     * <p>
     * <b>Access Control:</b> Users can fetch their own profile, but fetching others
     * requires Manager/Admin clearance within the same warehouse.
     * </p>
     * @param id User UUID.
     * @return User details mapped to a response DTO.
     */
    UserResponse getUserById(String id);

    /**
     * Performs a logical 'Soft-Delete' of a user account.
     * <p>
     * <b>Data Integrity:</b> The record is marked as {@link UserStatus#DELETED} to
     * maintain historical referential integrity for warehouse transactions.
     * </p>
     * @param id User UUID to be deactivated.
     * @throws com.infotact.warehouse.exception.UnauthorizedException if requester is not an ADMIN.
     */
    void deleteUser(String id);

    /**
     * Filters staff by specialized role within the current warehouse scope.
     * @param role The target {@link Role} (e.g., MANAGER, EMPLOYEE).
     * @return A list of matching users in the requester's warehouse.
     */
    List<UserResponse> getUsersByRole(Role role);

    /**
     * Lists all staff currently cleared (ACTIVE) for warehouse operations.
     * <p>
     * Used primarily for task assignment (picking, packing, receiving).
     * </p>
     * @return List of active UserResponse objects.
     */
    List<UserResponse> getAllActiveUsers();

    /**
     * Retrieves the profile of the currently authenticated user.
     * <p>
     * Used for the "My Profile" dashboard view without requiring the frontend
     * to manage the user's UUID.
     * </p>
     * @return The profile of the session holder.
     */
    UserResponse getMyProfile();
}