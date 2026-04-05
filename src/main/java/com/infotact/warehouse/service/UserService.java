package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service Interface for User Management.
 * Handles the creation, lifecycle, and security scoping of staff across different warehouses.
 */
public interface UserService {

    /**
     * Creates a new user with an auto-generated temporary password.
     * @param request User details and assigned warehouse.
     * @return Success message including the assigned role.
     */
    String createUser(@Valid UserRequest request);

    /**
     * Retrieves users based on security context.
     * Super Admin gets global Admins; Admin/Manager gets warehouse-specific staff.
     * @return List of UserResponse DTOs.
     */
    List<UserResponse> getAllUser();

    /**
     * Toggles a user's status (ACTIVE/INACTIVE/DELETED).
     * @param id Target User UUID.
     * @param status Desired status.
     */
    void updateStatus(String id, UserStatus status);

    /**
     * Updates user profile info or promotes roles.
     * @param id Target User UUID.
     * @param request Updated details.
     * @return Success message.
     */
    String updateUserDetails(String id, UserUpdate request);

    /**
     * Fetches a single user DTO by ID.
     * @param id User UUID.
     * @return UserResponse DTO.
     */
    UserResponse getUserById(String id);

    /**
     * Marks a user as DELETED in the system.
     * @param id Target User UUID.
     */
    void deleteUser(String id);

    /**
     * Filters users in the current warehouse scope by role.
     * @param role Target Role enum.
     * @return List of UserResponse DTOs.
     */
    List<UserResponse> getUsersByRole(Role role);

    /**
     * Retrieves all users in the current scope with an ACTIVE status.
     * @return List of UserResponse DTOs.
     */
    List<UserResponse> getAllActiveUsers();
}