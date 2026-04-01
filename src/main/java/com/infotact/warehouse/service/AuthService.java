package com.infotact.warehouse.service;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;
import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for managing authentication and user administration.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns a JWT token.
     * * @param request the login credentials (email and password)
     * @return a JSON string containing the generated JWT token
     */
    String login(LoginRequest request);

    /**
     * Retrieves a list of all users in the system.
     * Access is restricted to users with ADMIN roles.
     * * @return a list of UserResponse objects
     */
    List<UserResponse> getAllUser();

    /**
     * Updates the status (enable/disable) of a specific user.
     * * @param user ID and the new status
     */
    void updateStatus(String id, Boolean status);


    String changePassword(ChangePasswordRequest request);

    String forgotPassword(String email);

    String resetPassword(ResetPasswordRequest request);

    String createUser(@Valid UserRequest request);
}