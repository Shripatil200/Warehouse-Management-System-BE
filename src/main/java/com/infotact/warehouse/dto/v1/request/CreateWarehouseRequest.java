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
 * Data Transfer Object for the verified system setup.
 * <p>
 * Requires verification tokens generated from the OTP flow to ensure
 * authenticity and prevent bot-driven warehouse creation.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Verified request payload to initialize a warehouse and the primary Admin user")
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
    @Schema(description = "Email used for login. Must match the email used for OTP verification.", example = "admin@infotact.com")
    private String adminEmail;

    @NotBlank(message = "Contact number is required")
    @Size(min = 10, max = 15)
    @Pattern(regexp = "^\\d+$", message = "Contact number must contain only digits")
    @Schema(description = "Primary contact number. Must match the number used for OTP verification.", example = "9876543210")
    private String adminContact;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "Secure password for the admin account", example = "SecurePass123!")
    private String password;

    // --- Verification Proofs (Critical for Security) ---

    @NotBlank(message = "Email verification token is missing")
    @Schema(description = "The eml-UUID token received after successful email OTP verification")
    private String emailToken;

    @NotBlank(message = "Contact verification token is missing")
    @Schema(description = "The cnt-UUID token received after successful contact OTP verification")
    private String contactToken;
}