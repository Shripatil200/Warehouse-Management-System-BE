package com.infotact.warehouse.config;

import com.infotact.warehouse.config.JWT.SupplierPrincipal;
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

/**
 * Activates the Hibernate {@code warehouseFilter} for warehouse staff requests.
 * <p>
 * If the authenticated principal is a {@link SupplierPrincipal}, the filter is
 * skipped entirely — suppliers are global and have no tenant scope.
 * If the principal is a {@link UserPrincipal}, the warehouseId is always present
 * (non-null) because {@code User.warehouse} is now non-nullable.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    private static final Set<String> PUBLIC_APIS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/warehouses/setup",
            "/api/v1/auth/otp/send-email",
            "/api/v1/auth/otp/verify-email",
            "/api/v1/auth/otp/send-contact",
            "/api/v1/auth/otp/verify-contact",
            "/api/v1/supplier/register",
            "/api/v1/supplier/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        try {
            if (isPublicApi(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }

            Object principal = authentication.getPrincipal();

            // Supplier — global, no tenant filter needed
            if (principal instanceof SupplierPrincipal) {
                log.debug("TenantFilter: skipping warehouse filter for supplier, path={}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Warehouse staff — activate tenant filter
            if (principal instanceof UserPrincipal userPrincipal) {
                String warehouseId = userPrincipal.getWarehouseId();
                log.debug("TenantFilter: warehouse={}, path={}", warehouseId, path);

                TenantContext.set(warehouseId);

                Session session = entityManager.unwrap(Session.class);
                if (session.getEnabledFilter("warehouseFilter") == null) {
                    session.enableFilter("warehouseFilter")
                            .setParameter("warehouseId", warehouseId);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("TenantFilter unexpected error for path {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal Server Error");
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicApi(String path) {
        return PUBLIC_APIS.contains(path)
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/webjars");
    }
}
