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

    // ✅ EXACT PUBLIC APIs (NO PREFIX MATCHING)
    private static final Set<String> PUBLIC_APIS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/warehouses/setup",
            "/api/v1/auth/otp/send-email",
            "/api/v1/auth/otp/verify-email",
            "/api/v1/auth/otp/send-contact",
            "/api/v1/auth/otp/verify-contact",
            "/v3/api-docs",
            "/swagger-ui"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        try {

            // 🔥 STEP 1: SKIP public APIs
            if (isPublicApi(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 🔐 STEP 2: Extract tenant (only for protected APIs)
            String warehouseId = extractWarehouseId();

            log.debug("TenantFilter → warehouse={}, path={}", warehouseId, path);

            TenantContext.set(warehouseId);

            // 🔥 STEP 3: Enable Hibernate filter
            Session session = entityManager.unwrap(Session.class);

            if (session.getEnabledFilter("warehouseFilter") == null) {
                session.enableFilter("warehouseFilter")
                        .setParameter("warehouseId", warehouseId);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("TenantFilter error", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User not authenticated");
        } finally {
            TenantContext.clear();
        }
    }

    // ============================================================
    // PUBLIC API CHECK (EXACT MATCH)
    // ============================================================

    private boolean isPublicApi(String path) {
        return PUBLIC_APIS.contains(path);
    }

    // ============================================================
    // TENANT EXTRACTION (UNCHANGED CORE LOGIC)
    // ============================================================

    private String extractWarehouseId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("User not authenticated");
        }

        String warehouseId = principal.getWarehouseId();

        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalStateException("No warehouseId in authenticated user");
        }

        return warehouseId;
    }
}