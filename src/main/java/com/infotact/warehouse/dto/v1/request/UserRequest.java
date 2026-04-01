package com.infotact.warehouse.dto.v1.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    String contactNumber;

    // Optional: If you want the Manager to specify a role,
    // otherwise it defaults to EMPLOYEE in the ServiceImpl
    String role;
}
