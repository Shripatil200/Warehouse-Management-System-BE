package com.infotact.warehouse.service;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;

/**
 * Service interface for managing authentication, JWT issuance, and credential security.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns a JWT token.
     * Validates account status to ensure only ACTIVE users can enter the system.
     *
     * @param request the login credentials (email and password).
     * @return a JSON string containing the generated JWT token.
     */
    String login(LoginRequest request);

    /**
     * Updates the password for the currently logged-in user.
     *
     * @param request Request containing old and new passwords.
     * @return Success message.
     */
    String changePassword(ChangePasswordRequest request);

    /**
     * Generates a unique UUID token for password recovery and sends it via email.
     *
     * @param email The target user's email address.
     * @return Notification message.
     */
    String forgotPassword(String email);

    /**
     * Validates a reset token and updates the user's password in the database.
     *
     * @param request Request containing the token and the new password.
     * @return Success message.
     */
    String resetPassword(ResetPasswordRequest request);
}