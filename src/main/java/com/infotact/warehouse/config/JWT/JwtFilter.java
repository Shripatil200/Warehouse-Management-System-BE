package com.infotact.warehouse.config.JWT;

import com.infotact.warehouse.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter.
 * <p>
 * Resolves the principal by checking the {@code users} table first via
 * {@link UsersDetailsService}. If the email is not found there, it falls back
 * to {@link SupplierDetailsService} which checks the {@code suppliers} table.
 * This keeps the two authentication domains (warehouse staff vs suppliers)
 * completely separate while sharing a single JWT format.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil               jwtUtil;
    private final UsersDetailsService   usersDetailsService;
    private final SupplierDetailsService supplierDetailsService;
    private final TokenBlacklistService  tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String token    = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                log.error("JWT parse error: {}", e.getMessage());
            }
        }

        // Reject explicitly revoked tokens (logout / forced re-auth)
        if (token != null && tokenBlacklistService.isBlacklisted(token)) {
            log.warn("Rejected blacklisted token for request to {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked. Please log in again.");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = resolveUserDetails(username);

            if (userDetails != null && jwtUtil.validateToken(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Tries warehouse users first, then falls back to suppliers.
     * Returns null if the email is not found in either table.
     */
    private UserDetails resolveUserDetails(String email) {
        try {
            return usersDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException ex) {
            // Not a warehouse user — try supplier table
        }
        try {
            return supplierDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException ex) {
            log.warn("No user or supplier found for email: {}", email);
            return null;
        }
    }
}
