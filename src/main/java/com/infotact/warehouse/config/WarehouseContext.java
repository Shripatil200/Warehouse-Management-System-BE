package com.infotact.warehouse.config;

import com.infotact.warehouse.config.JWT.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Provides the warehouse ID for the currently authenticated user.
 * Reads directly from the JWT principal stored in the Spring Security context.
 *
 * <p>Because this is a single-warehouse system, every authenticated staff member
 * belongs to the same warehouse. The ID is embedded in their JWT at login.</p>
 */
@Component
public class WarehouseContext {

    /**
     * Returns the warehouse ID of the authenticated user.
     *
     * @throws IllegalStateException if the caller is not an authenticated warehouse user.
     */
    public String getWarehouseId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            String id = principal.getWarehouseId();
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        throw new IllegalStateException(
                "No authenticated warehouse user found in security context.");
    }
}
