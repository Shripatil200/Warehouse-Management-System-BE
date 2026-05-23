package com.infotact.warehouse.dto.v1.response;

import lombok.Builder;
import lombok.Data;

/**
 * Authentication response for both warehouse staff and supplier logins.
 * <p>
 * For warehouse staff: {@code warehouseId} is populated, {@code supplierId} is null.
 * For suppliers: {@code supplierId} is populated, {@code warehouseId} is null.
 * </p>
 */
@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String role;
    /** Populated for warehouse staff logins. Null for supplier logins. */
    private String warehouseId;
    /** Populated for supplier logins. Null for warehouse staff logins. */
    private String supplierId;
}
