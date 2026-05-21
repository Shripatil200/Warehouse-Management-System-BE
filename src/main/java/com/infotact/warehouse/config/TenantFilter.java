package com.infotact.warehouse.config;

import com.infotact.warehouse.config.JWT.UserPrincipal;
import com.infotact.warehouse.entity.enums.Role;
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
     * Paths that bypass tenant resolution entirely (no authentication required).
     */
    private static final Set<String> PUBLIC_APIS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/warehouses/setup",
            "/api/v1/auth/otp/send-email",
            "/api/v1/auth/otp/verify-email",
            "/api/v1/auth/otp/send-contact",
            "/api/v1/auth/otp/verify-contact",
            "/api/v1/supplier/register"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        try {
            if (isPublicApi(path)) {
                log.debug("TenantFilter skipping public path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                throw new IllegalStateException("No valid authentication found");
            }

            // SUPPLIER users are global — they have no warehouse context.
            // Skip Hibernate filter activation; they access only supplier-scoped endpoints.
            if (isSupplierRole(principal)) {
                log.debug("TenantFilter skipping warehouse filter for SUPPLIER: {}, path={}", principal.getUsername(), path);
                filterChain.doFilter(request, response);
                return;
            }

            String warehouseId = principal.getWarehouseId();
            if (warehouseId == null || warehouseId.isBlank()) {
                throw new IllegalStateException("No warehouseId found for the authenticated user");
            }

            log.debug("TenantFilter → warehouse={}, path={}", warehouseId, path);

            TenantContext.set(warehouseId);

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
            TenantContext.clear();
        }
    }

    private boolean isPublicApi(String path) {
        if (PUBLIC_APIS.contains(path)) {
            return true;
        }
        return path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.contains("/webjars");
    }

    private boolean isSupplierRole(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.SUPPLIER.name()));
    }
}
