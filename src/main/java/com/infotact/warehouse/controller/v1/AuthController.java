package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;
import com.infotact.warehouse.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and security-related operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user login, password recovery, and credential management")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User Login", description = "Authenticates credentials and returns a JWT token if the account is ACTIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, returns token"),
            @ApiResponse(responseCode = "400", description = "Invalid email/password"),
            @ApiResponse(responseCode = "403", description = "Account is not ACTIVE (DELETED or INACTIVE)")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

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

    @Operation(summary = "Forgot Password", description = "Triggers an email containing a reset token to the user's registered email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset link sent successfully (even if email doesn't exist for security)"),
            @ApiResponse(responseCode = "400", description = "Error sending reset email")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Parameter(description = "Registered email address", example = "user@infotact.com")
            @Valid @RequestParam String email) {
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    @Operation(summary = "Reset Password", description = "Resets the user's password using the token received via email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}