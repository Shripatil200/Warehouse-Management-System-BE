package com.infotact.warehouse.common_wrappers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user authentication requests.
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(description = "Login Credentials")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(description = "Registered email address", example = "admin@warehouse.com")
    private String email;

    @NotBlank
    @Schema(description = "Account password", example = "Welcome@1234")
    private String password;
}
