package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing warehouse personnel and administrative accounts.
 * <p>
 * This controller handles the lifecycle of users, including registration, status updates,
 * and profile management. Access is partitioned by the requester's warehouse to
 * ensure strict multi-tenant data privacy (Silo Pattern).
 * </p>
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "1. User Management", description = "Operations for managing warehouse staff and administrative accounts")
public class UserController {

    private final UserService userService;

    /**
     * Registers a new staff member or administrator.
     * <p>
     * Logic: Managers can only create staff for their own warehouse.
     * Administrative roles can only be granted by existing Administrators.
     * </p>
     *
     * @param request The registration details (email, role, contact, warehouseId).
     * @return A success message confirming account creation.
     */
    @Operation(
            summary = "Register a new staff member",
            description = "Creates a new user record. Warehouse isolation is enforced. Managers cannot create ADMIN/MANAGER roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient permissions for role assignment")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<String> createEmployee(@Valid @RequestBody UserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * * @return The UserResponse of the session holder.
     */
    @Operation(summary = "Get my profile", description = "Retrieves the profile of the currently logged-in user.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    /**
     * Retrieves a paginated list of all users within the requester's warehouse.
     *
     * @param pageable pagination parameters (page, size, sort).
     * @return A Page of UserResponse objects.
     */
    @Operation(summary = "Retrieve all users (Paginated)", description = "Fetches a paginated list of users belonging to the requester's warehouse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully")
    })
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<UserResponse>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUser(pageable));
    }

    /**
     * Fetches detailed information for a specific user.
     *
     * @param id The UUID of the user.
     * @return The user details if the requester has warehouse access.
     */
    @Operation(summary = "Get user by ID", description = "Fetches details for a user. Access is restricted to the user's warehouse context.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "The unique UUID of the user", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Filters users within the current warehouse by their system role.
     *
     * @param role The role to filter by (e.g., ADMIN, MANAGER, EMPLOYEE).
     * @return List of matching users.
     */
    @Operation(summary = "Filter users by role", description = "Retrieves users by role within the warehouse scope.")
    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getByRole(
            @Parameter(description = "System role enum value", example = "MANAGER")
            @PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    /**
     * Updates the operational status of a user (e.g., ACTIVE, INACTIVE).
     *
     * @param id The user's UUID.
     * @param status The new status to apply.
     */
    @Operation(summary = "Update user status", description = "Activates or deactivates a user record. Managers cannot modify other Managers/Admins.")
    @PatchMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateStatus(
            @Parameter(description = "The UUID of the user") @RequestParam String id,
            @Parameter(description = "Target status") @RequestParam UserStatus status) {
        userService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates user profile information.
     * <p>
     * Users can update their own details. Managers can update employees.
     * Role changes are strictly restricted to ADMINs in the service layer.
     * </p>
     */
    @Operation(summary = "Update user details", description = "Updates profile fields. Role changes require ADMIN role.")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @Valid @RequestBody UserUpdate request) {
        return ResponseEntity.ok(userService.updateUserDetails(id, request));
    }

    /**
     * Performs a soft delete on a user account.
     * <p>
     * Restriction: Strictly restricted to the <b>ADMIN</b> role.
     * Account is marked as 'DELETED' to preserve audit history.
     * </p>
     */
    @Operation(summary = "Soft delete user", description = "Marks a user record as DELETED. Restricted to ADMIN role. Self-deletion is blocked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete own account"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient permissions")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves all active users for operational assignments.
     */
    @Operation(summary = "Get all active users", description = "Retrieves ACTIVE users within the warehouse scope for task assignment.")
    @GetMapping("/all-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllActiveUsers(){
        return ResponseEntity.ok(userService.getAllActiveUsers());
    }
}