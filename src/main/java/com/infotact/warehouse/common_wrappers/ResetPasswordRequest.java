package com.infotact.warehouse.common_wrappers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for finalizing the password reset using a secure token received via email.
 */
@Data
@Schema(description = "Request body for resetting password via email recovery token")
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "The unique UUID token received in the recovery email",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @Schema(description = "The new password to be applied to the account",
            example = "RecoveryPass@99", minLength = 8)
    private String newPassword;
}