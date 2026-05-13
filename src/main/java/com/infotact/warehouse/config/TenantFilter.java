package com.infotact.warehouse.config;

import com.infotact.warehouse.config.JWT.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    /**
     * Set of exact paths that do not require a tenant/warehouse context.
     * Note: Swagger UI sub-resources are handled via prefix matching in isPublicApi().
     */
    private static final Set<String> PUBLIC_APIS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/warehouses/setup",
            "/api/v1/auth/otp/send-email",
            "/api/v1/auth/otp/verify-email",
            "/api/v1/auth/otp/send-contact",
            "/api/v1/auth/otp/verify-contact"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Using getServletPath() is more reliable for matching routes in Spring Boot
        String path = request.getServletPath();

        try {
            // 🔥 STEP 1: Skip tenant logic for public APIs and Swagger resources
            if (isPublicApi(path)) {
                log.debug("TenantFilter skipping public path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // 🔐 STEP 2: Extract tenant (only for protected APIs)
            // This will throw IllegalStateException if the user isn't logged in
            String warehouseId = extractWarehouseId();

            log.debug("TenantFilter → warehouse={}, path={}", warehouseId, path);

            // Set the ID in the ThreadLocal context
            TenantContext.set(warehouseId);

            // 🔥 STEP 3: Enable Hibernate data isolation filter
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("warehouseFilter") == null) {
                session.enableFilter("warehouseFilter")
                        .setParameter("warehouseId", warehouseId);
            }

            filterChain.doFilter(request, response);

        } catch (IllegalStateException e) {
            log.warn("TenantFilter authentication check failed for path {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User not authenticated");
        } catch (Exception e) {
            log.error("TenantFilter unexpected error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal Server Error");
        } finally {
            // Always clear the context to prevent memory leaks or data cross-contamination
            TenantContext.clear();
        }
    }

    /**
     * Determines if a path should bypass the Tenant Filter.
     * Includes exact matches for Auth and prefix/contains matching for Swagger UI.
     */
    private boolean isPublicApi(String path) {
        // 1. Check exact matches (Auth endpoints)
        if (PUBLIC_APIS.contains(path)) {
            return true;
        }

        // 2. Check for Swagger/OpenAPI resources (which use dynamic sub-paths for CSS/JS)
        return path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.contains("/webjars");
    }

    /**
     * Retrieves the warehouseId from the current SecurityContext.
     */
    private String extractWarehouseId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No valid authentication found");
        }

        String warehouseId = principal.getWarehouseId();

        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalStateException("No warehouseId found for the authenticated user");
        }

        return warehouseId;
    }
}