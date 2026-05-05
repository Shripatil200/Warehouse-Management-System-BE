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

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String warehouseId = extractWarehouseId();

            log.debug("TenantFilter → warehouse={}, path={}",
                    warehouseId, request.getRequestURI());

            TenantContext.set(warehouseId);

            Session session = entityManager.unwrap(Session.class);

            if (session.getEnabledFilter("warehouseFilter") == null) {
                session.enableFilter("warehouseFilter")
                        .setParameter("warehouseId", warehouseId);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("TenantFilter error", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } finally {
            TenantContext.clear();
        }
    }

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