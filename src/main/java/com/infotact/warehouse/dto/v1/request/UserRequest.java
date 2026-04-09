package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Data Transfer Object for registering new warehouse personnel.
 * <p>
 * This request captures personal details and maps the user to a specific facility.
 * Security Note: Managers can only onboard 'EMPLOYEE' roles for their own warehouse,
 * while Admins have broader registration privileges.
 * </p>
 */
@Data
@Schema(
        name = "UserRequest",
        description = "Payload for onboarding new staff members or administrators"
)
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(
            description = "Full name of the staff member",
            example = "Amit Sharma",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(
            description = "Work email address (used for login and notifications)",
            example = "amit.s@infotact.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(
            description = "10-digit primary mobile number",
            example = "9876543210",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String contactNumber;

    @Schema(
            description = "System role for the user. Defaults to 'EMPLOYEE' if not specified.",
            example = "EMPLOYEE",
            allowableValues = {"ADMIN", "MANAGER", "EMPLOYEE"}
    )
    private String role;

    @NotBlank(message = "Warehouse ID is required")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid Warehouse ID format (must be a valid UUID)"
    )
    @Schema(
            description = "The UUID of the warehouse facility this user is assigned to",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String warehouseId;
}