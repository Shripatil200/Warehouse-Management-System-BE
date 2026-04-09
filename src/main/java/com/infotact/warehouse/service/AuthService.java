package com.infotact.warehouse.service;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;
import com.infotact.warehouse.dto.v1.response.AuthResponse; // New Import

/**
 * Service interface for Identity and Access Management (IAM).
 * <p>
 * This service acts as the security gateway for the Infotact WMS. It manages
 * the end-to-end credential lifecycle, including secure authentication,
 * session issuance via JWT, and self-service account recovery.
 * </p>
 */
public interface AuthService {

    /**
     * Authenticates a user and issues a structured session response.
     * <p>
     * <b>Security Logic:</b>
     * 1. Verifies credentials against Bcrypt-encoded passwords.
     * 2. Strict Status Check: Rejects authentication if user status is not 'ACTIVE'.
     * 3. Claims Injection: Embeds 'Role' and 'WarehouseID' into the JWT.
     * </p>
     * @param request The login credentials (email and password).
     * @return An {@link AuthResponse} containing the JWT and user context.
     * @throws com.infotact.warehouse.exception.UnauthorizedException if credentials or status are invalid.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Updates the password for an authenticated session.
     * <p>
     * <b>Validation:</b> Must verify that the 'oldPassword' matches the
     * current database hash before applying the new password to prevent account hijacking.
     * </p>
     * @param request Request containing current and replacement passwords.
     * @return A confirmation message upon successful persistence.
     */
    String changePassword(ChangePasswordRequest request);

    /**
     * Initiates the asynchronous password recovery workflow.
     * <p>
     * <b>Workflow:</b>
     * 1. Generates a cryptographically secure, time-bound Reset Token.
     * 2. Persists the token via {@link com.infotact.warehouse.repository.ResetPasswordRepository}.
     * 3. Triggers an out-of-band notification (Email) containing the recovery link.
     * </p>
     * @param email The target user's registered email address.
     * @return A generic notification message to prevent 'User Enumeration' attacks.
     */
    String forgotPassword(String email);

    /**
     * Completes the account recovery process using a valid token.
     * <p>
     * <b>Security Logic:</b>
     * Verifies token existence and expiration. Once consumed, the token is
     * invalidated to prevent 'Replay Attacks'.
     * </p>
     * @param request Request containing the unique recovery token and new password.
     * @return Success message.
     * @throws com.infotact.warehouse.exception.BadRequestException if the token is invalid or expired.
     */
    String resetPassword(ResetPasswordRequest request);
}