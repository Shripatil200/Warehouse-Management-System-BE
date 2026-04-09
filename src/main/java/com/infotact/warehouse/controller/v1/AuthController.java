package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;
import com.infotact.warehouse.dto.v1.response.AuthResponse; // New Import
import com.infotact.warehouse.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication and security-related operations.
 * <p>
 * This controller handles the lifecycle of user sessions and credential security.
 * Most endpoints here are public (login, forgot-password) to allow user entry.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "0. Authentication", description = "Endpoints for user login, password recovery, and credential management")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and generates a session object.
     * <p>
     * Logic: Validates credentials and checks account status. Returns an object
     * containing the JWT and user metadata (Role, Warehouse) for client-side routing.
     * </p>
     *
     * @param request Contains email and password.
     * @return An {@link AuthResponse} containing the session token and user context.
     */
    @Operation(summary = "User Login", description = "Authenticates credentials and returns a JWT token and user metadata if the account is ACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, returns session object",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email/password"),
            @ApiResponse(responseCode = "403", description = "Account is not ACTIVE (DELETED or INACTIVE)")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Updates the password for the currently logged-in user.
     * <p>
     * This endpoint requires an 'Authorization' header. It compares the
     * provided 'oldPassword' with the hashed value in the database.
     * </p>
     */
    @Operation(summary = "Change Password", description = "Allows an authenticated user to update their password by providing the old one.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Incorrect old password"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }

    /**
     * Initiates the password recovery process.
     * <p>
     * Sends an email with a unique token. For security against user enumeration,
     * this endpoint always returns 200 OK even if the email does not exist.
     * </p>
     *
     * @param request Map containing the target email.
     */
    @Operation(summary = "Forgot Password", description = "Triggers an email containing a reset token to the user's registered email address.")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON containing the user email",
                    content = @Content(schema = @Schema(example = "{\"email\": \"user@example.com\"}")))
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    /**
     * Finalizes the password recovery process.
     * <p>
     * Consumes the reset token received via email and updates the user's password.
     * </p>
     */
    @Operation(summary = "Reset Password", description = "Resets the user's password using the token received via email.")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}