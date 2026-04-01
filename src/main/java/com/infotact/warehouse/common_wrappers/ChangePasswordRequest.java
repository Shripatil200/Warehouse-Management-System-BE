package com.infotact.warehouse.common_wrappers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating an existing user's password.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String newPassword;
}