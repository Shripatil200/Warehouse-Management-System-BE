package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for the initial system setup.
 * <p>
 * This request simultaneously creates the first physical warehouse entity
 * and the primary Administrative user who will manage the system.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to initialize the primary warehouse facility and the first Admin user")
public class CreateWarehouseRequest {

    // --- Warehouse Details ---

    @NotBlank(message = "Warehouse name is required")
    @Schema(description = "The official name of the warehouse facility", example = "Infotact Central Hub")
    private String name;

    @NotBlank(message = "Location is required")
    @Schema(description = "Physical address or city of the warehouse", example = "Hyderabad, India")
    private String location;

    // --- Admin User Details ---

    @NotBlank(message = "Admin name is required")
    @Schema(description = "Full name of the primary administrator", example = "John Doe")
    private String adminName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Admin email is required")
    @Schema(description = "Email address for the admin account (used for login and recovery)", example = "admin@infotact.com")
    private String adminEmail;

    @NotBlank(message = "Contact number is required")
    @Size(min = 10, max = 15)
    @Pattern(regexp = "^\\d+$", message = "Contact number must contain only digits")
    @Schema(description = "Primary contact number. The last 4 digits will be used to generate the default password.", example = "9876543210")
    private String adminContact;
}