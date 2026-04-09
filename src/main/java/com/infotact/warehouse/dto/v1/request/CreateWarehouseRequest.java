package com.infotact.warehouse.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initialize the warehouse and the first Admin user")
public class WarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Admin name is required")
    private String adminName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Admin email is required")
    private String adminEmail;

    @NotBlank(message = "Contact number is required")
    @Size(min = 10, max = 15)
    @Pattern(regexp = "^\\d+$", message = "Contact number must contain only digits")
    private String adminContact;
}