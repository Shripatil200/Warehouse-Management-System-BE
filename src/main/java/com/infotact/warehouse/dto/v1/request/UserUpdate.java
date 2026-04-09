package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for updating existing user profiles.
 * <p>
 * All fields are optional. Only the fields provided in the request body
 * will be updated in the database.
 * Note: Role updates are internally restricted to ADMIN users within the service layer.
 * </p>
 */
@Data
@Schema(
        name = "UserUpdate",
        description = "Payload for updating existing user information. Fields left null will remain unchanged."
)
public class UserUpdate {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "Updated full name of the user", example = "Amit K. Sharma")
    private String name;

    @Email(message = "Invalid email format")
    @Schema(description = "Updated work email address", example = "amit.new@infotact.com")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Schema(description = "Updated 10-digit mobile number", example = "9876543211")
    private String contactNumber;

    @Schema(
            description = "Updated system role. Restricted to ADMIN usage.",
            example = "MANAGER",
            allowableValues = {"ADMIN", "MANAGER", "EMPLOYEE"}
    )
    private String role;

    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid Warehouse ID format"
    )
    @Schema(
            description = "The UUID of the new warehouse facility if the user is being transferred",
            example = "b2c3d4e5-f6a7-8901-bcde-f01234567891"
    )
    private String warehouseId;
}