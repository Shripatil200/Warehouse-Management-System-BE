package com.infotact.warehouse.dto.v1.response;

import lombok.Builder;
import lombok.Data;

/**
 * Encapsulates authentication credentials and basic user context.
 * <p>
 * This structured response allows the client to persist essential session
 * metadata (like Warehouse identity) without redundant JWT decoding.
 * </p>
 */
@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private String warehouseId;
}