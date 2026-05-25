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

/**
 * Servlet filter that activates the Hibernate {@code warehouseFilter} for every
 * authenticated request, ensuring all queries are automatically scoped to the
 * warehouse the requesting user belongs to.
 *
 * <p>The warehouse ID is extracted from the JWT principal (set by
 * {@link JWT.JwtFilter} earlier in the chain) and stored in
 * {@link WarehouseContext} for use by service-layer components during the
 * same request thread.</p>
 *
 * <p>The context is always cleared in the {@code finally} block to prevent
 * thread-pool reuse from leaking one request's warehouse scope into the next.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseContextFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

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

            if (principal instanceof UserPrincipal userPrincipal) {
                String warehouseId = userPrincipal.getWarehouseId();
                log.debug("WarehouseContextFilter: warehouse={}, path={}", warehouseId, path);

                WarehouseContext.set(warehouseId);

                Session session = entityManager.unwrap(Session.class);
                if (session.getEnabledFilter("warehouseFilter") == null) {
                    session.enableFilter("warehouseFilter")
                            .setParameter("warehouseId", warehouseId);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("WarehouseContextFilter unexpected error for path {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal Server Error");
        } finally {
            WarehouseContext.clear();
        }
    }

    private boolean isPublicApi(String path) {
        return PUBLIC_APIS.contains(path)
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/webjars");
    }
}
