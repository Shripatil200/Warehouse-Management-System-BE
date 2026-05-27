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
 * JWT authentication filter for warehouse staff.
 *
 * <p>Validates the token type claim ("USER") so that any future token types
 * (e.g. service-to-service, refresh) are rejected at the boundary.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil               jwtUtil;
    private final UsersDetailsService   usersDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

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
        } else if (path.equals("/api/v1/tasks/stream") && request.getParameter("token") != null) {
            token = request.getParameter("token");
        }

        if (token != null) {
            try {
                // Validate token type — only USER tokens are accepted here
                String type = jwtUtil.extractClaims(token,
                        claims -> claims.get("type", String.class));
                if (!"USER".equals(type)) {
                    log.warn("Rejected non-USER token type '{}' for path {}", type, path);
                    filterChain.doFilter(request, response);
                    return;
                }
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                log.error("JWT parse error: {}", e.getMessage());
            }
        }

        // Reject explicitly revoked tokens (logout / forced re-auth)
        if (token != null && tokenBlacklistService.isBlacklisted(token)) {
            log.warn("Rejected blacklisted token for request to {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Token has been revoked. Please log in again.");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = usersDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (UsernameNotFoundException ex) {
                log.warn("No user found for email: {}", username);
            }
        }

        filterChain.doFilter(request, response);
    }
}
