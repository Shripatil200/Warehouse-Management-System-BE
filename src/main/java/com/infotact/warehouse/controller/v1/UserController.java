package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations for managing warehouse staff and administrative accounts")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Register a new staff member",
            description = "Creates a new user record. Restricted to ADMIN and MANAGER. Managers can only create staff for their specific warehouse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<String> createEmployee(@Valid @RequestBody UserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Retrieve all users in scope",
            description = "Super Admins see all Admins; Admins and Managers see users within their assigned warehouse."
    )
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAllUser());
    }

    @Operation(summary = "Get user details by ID", description = "Fetches details for a specific user. Requester must have access to the user's warehouse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful retrieval"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "UUID of the user", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Filter users by role", description = "Retrieves a list of users filtered by their role (EMPLOYEE, MANAGER, etc.) within the warehouse.")
    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getByRole(
            @Parameter(description = "User role enum value") @PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @Operation(summary = "Update user status", description = "Activates or deactivates a user record. Restricted to ADMIN role.")
    @PatchMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(
            @Parameter(description = "User UUID") @RequestParam String id,
            @Parameter(description = "Target status (ACTIVE, INACTIVE, etc.)") @RequestParam UserStatus status) {
        userService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user details or role", description = "Updates profile fields or promotes/demotes a user. Restricted to ADMIN.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> update(
            @Parameter(description = "User UUID") @PathVariable String id,
            @Valid @RequestBody UserUpdate request) {
        return ResponseEntity.ok(userService.updateUserDetails(id, request));
    }

    @Operation(summary = "Soft delete user", description = "Marks a user record as DELETED. Restricted to ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "User UUID") @PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all active users", description = "Retrieves only users with an ACTIVE status within the warehouse scope.")
    @GetMapping("/all-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllActiveUsers(){
        return ResponseEntity.ok(userService.getAllActiveUsers());
    }
}