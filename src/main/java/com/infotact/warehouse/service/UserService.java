package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for Identity Management and Staff Orchestration.
 * <p>
 * This service manages the staff lifecycle, role-based access control (RBAC),
 * and multi-tenant security boundaries. It ensures that staff assignments
 * and administrative actions remain strictly within the authorized facility scope.
 * </p>
 */
public interface UserService {

    /**
     * Provisions a new staff member and triggers onboarding.
     * <p>
     * <b>Onboarding Logic:</b>
     * 1. <b>Password Generation:</b> Creates a temporary, deterministic password
     * based on the warehouse's welcome policy.
     * 2. <b>Security Boundary:</b> Binds the user to a specific {@link com.infotact.warehouse.entity.Warehouse}.
     * 3. <b>Notification:</b> Triggers an onboarding email via the Mail Utility.
     * </p>
     * @param request User profile and facility assignment details.
     * @return A success message confirming account creation.
     */
    String createUser(@Valid UserRequest request);

    /**
     * Retrieves staff members based on the requester's security clearance.
     * <p>
     * <b>Visibility Rules:</b>
     * 1. <b>Super Admin:</b> Can view Global Admins and facility managers across all warehouses.
     * 2. <b>Warehouse Admin/Manager:</b> Can only view staff registered to their own facility.
     * </p>
     * @return A filtered list of user profiles.
     */
    List<UserResponse> getAllUser();

    /**
     * Manages the operational availability of a staff member.
     * <p>
     * <b>Safety Check:</b> Prevents an Admin from deactivating their own account
     * to avoid system lockout.
     * </p>
     * @param id Target User UUID.
     * @param status New status (ACTIVE, INACTIVE, etc.).
     */
    void updateStatus(String id, UserStatus status);

    /**
     * Updates profile metadata or modifies authorization levels (Roles).
     * <p>
     * <b>Validation:</b> Ensures that role promotions (e.g., Staff to Manager)
     * are performed by a user with higher or equal authority.
     * </p>
     */
    String updateUserDetails(String id, UserUpdate request);

    /**
     * Fetches detailed profile information for a specific staff member.
     * @param id User UUID.
     * @return User details mapped to a response DTO.
     */
    UserResponse getUserById(String id);

    /**
     * Performs a logical 'Soft-Delete' of a user account.
     * <p>
     * <b>Data Integrity:</b> The record is marked as 'DELETED' but remains in
     * the database to maintain referential integrity for historical
     * Warehouse Transactions (e.g., "Who picked this order?").
     * </p>
     */
    void deleteUser(String id);

    /**
     * Filters staff by specialized role within the current warehouse scope.
     * @param role The target {@link Role} (e.g., PICKER, MANAGER).
     */
    List<UserResponse> getUsersByRole(Role role);

    /**
     * Lists all staff currently cleared for warehouse operations.
     * <p>
     * Usage: Used to populate assignment dropdowns for picking and receiving tasks.
     * </p>
     */
    List<UserResponse> getAllActiveUsers();
}