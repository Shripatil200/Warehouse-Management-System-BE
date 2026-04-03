package com.infotact.warehouse.dto.v1.request;

import com.infotact.warehouse.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    private String contactNumber;

    /**
     * Optional: If the manager provides a role, ensure it's not just whitespace.
     * Note: In your Service, you default this to EMPLOYEE if null.
     */
    private String role;

    /**
     * REQUIRED: Since a user must belong to a warehouse.
     * Validates that the ID is provided and follows a UUID pattern.
     */
    @NotBlank(message = "Warehouse ID is required")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid Warehouse ID format (must be a valid UUID)"
    )
    private String warehouseId;
}