package com.infotact.warehouse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @deprecated Use {@link WarehouseContextFilter} instead.
 * Retained only to prevent Spring from throwing a "no bean of type TenantFilter" error
 * if any external configuration still references this name. It delegates all work to
 * {@link WarehouseContextFilter} and will be removed in the next cleanup pass.
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final WarehouseContextFilter delegate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        delegate.doFilter(request, response, filterChain);
    }
}
