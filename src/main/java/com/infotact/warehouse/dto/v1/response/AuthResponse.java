package com.infotact.warehouse.dto.v1.response;

import lombok.Builder;
import lombok.Data;

/**
 * Authentication response for warehouse staff logins.
 */
@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private String warehouseId;
}
