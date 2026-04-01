package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;
import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for authentication and user management operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Retrieves all users (Admin only).
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUser() {
        return ResponseEntity.ok(authService.getAllUser());
    }

    /**
     * Updates a user's account status (Admin only).
     */
    @PatchMapping("/update-status")
    public ResponseEntity<String> updateUserStatus(@RequestParam String id, @RequestParam Boolean status) {
        authService.updateStatus(id, status);
        return ResponseEntity.ok("User status updated successfully");
    }

    /**
     * Changes the password for the currently authenticated user.
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }

    /**
     * Triggers the forgot password email flow.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestParam String email) {
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    /**
     * Resets the password using a valid token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    /**
     * Endpoint for Managers/Admins to create new employee accounts.
     * Accessible only by users with 'MANAGER' or 'ADMIN' roles.
     */
    @PostMapping("/create-employee")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> createEmployee(@Valid @RequestBody UserRequest request) {
        String response = authService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}