package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing warehouse personnel and administrative accounts.
 * <p>
 * Handles the lifecycle of users with strict multi-tenant data privacy (Silo Pattern).
 * All data access is automatically scoped to the authenticated user's warehouse facility.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "1. User Management", description = "Operations for managing warehouse staff and administrative accounts")
public class UserController {

    private final UserService userService;

    /**
     * Unified Advanced Search API.
     * <p>
     * Dynamic filtering automatically scoped to the requester's warehouse facility.
     * Supports fuzzy search on name/email and exact matching for role and status.
     * </p>
     */
    @Operation(
            summary = "Search users with filters",
            description = "Advanced dynamic filtering. Results are strictly siloed to the requester's warehouse."
    )
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @Parameter(description = "Fuzzy search by name or email") @RequestParam(required = false) String query,
            @Parameter(description = "Filter by system role") @RequestParam(required = false) Role role,
            @Parameter(description = "Filter by account status") @RequestParam(required = false) UserStatus status,
            @ParameterObject @PageableDefault(size = 15, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(query, role, status, pageable));
    }

    /**
     * Registers a new staff member.
     * <p>
     * Hierarchy Enforcement: Managers can only create 'EMPLOYEE' roles.
     * Admins can provision any role. Assignment is locked to the requester's warehouse.
     * </p>
     */
    @Operation(summary = "Register a new staff member")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already exists"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Hierarchy or Silo violation")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<String> createEmployee(@Valid @RequestBody UserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @Operation(summary = "Get my profile", description = "Fetches personal profile details of the current session holder.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    /**
     * Fetches detailed information for a specific user.
     */
    @Operation(summary = "Get user by ID", description = "Access restricted to warehouse silo. Regular employees can only view their own profile.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "UUID of the user", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Updates the operational status of a user.
     * <p>Note: Users are prevented from deactivating their own accounts.</p>
     */
    @Operation(summary = "Update user status", description = "Managers cannot modify status of other Managers or Admins.")
    @PatchMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> updateStatus(
            @Parameter(description = "UUID of the user") @RequestParam String id,
            @Parameter(description = "Target status") @RequestParam UserStatus status) {
        userService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates user profile information.
     * <p>Role modifications are strictly reserved for the ADMIN role in the service layer.</p>
     */
    @Operation(summary = "Update user details", description = "Allows metadata updates. Role promotion requires ADMIN clearance.")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @Valid @RequestBody UserUpdate request) {
        return ResponseEntity.ok(userService.updateUserDetails(id, request));
    }

    /**
     * Performs a soft delete on a user account.
     * <p>Credentials (email/phone) are released upon deletion to allow re-use.</p>
     */
    @Operation(summary = "Soft delete user", description = "Strictly restricted to ADMIN. Record is marked as DELETED for audit integrity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete own account"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient privileges")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}