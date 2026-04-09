package com.infotact.warehouse.common_wrappers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating an existing user's password.
 * Requires the current password to verify identity before applying the change.
 */
@Data
@Schema(description = "Request body for password updates while logged in")
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    @Schema(description = "The user's current password", example = "OldPass@123")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @Schema(description = "The new password to set", example = "NewSecurePass@2026", minLength = 8)
    private String newPassword;
}