package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for onboarding a new staff member into the warehouse.
 *
 * <p>The warehouse assignment is resolved automatically from the authenticated
 * admin's context — callers must <em>not</em> supply a warehouse ID.</p>
 */
@Data
@Schema(
        name = "UserRequest",
        description = "Payload for onboarding new staff members or administrators"
)
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "Full name of the staff member", example = "Amit Sharma",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Work email address (used for login and notifications)",
            example = "amit.s@infotact.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(description = "10-digit primary mobile number", example = "9876543210",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactNumber;

    @Schema(description = "System role for the user. Defaults to 'EMPLOYEE' if not specified.",
            example = "OPERATOR",
            allowableValues = {"ADMIN", "MANAGER", "OPERATOR", "EMPLOYEE"})
    private String role;
}
