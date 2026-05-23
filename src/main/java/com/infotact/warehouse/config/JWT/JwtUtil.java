package com.infotact.warehouse.config.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public String extractWarehouseId(String token) {
        return extractClaims(token, claims -> claims.get("warehouseId", String.class));
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Generates a JWT for a warehouse staff member.
     * Embeds {@code warehouseId} and {@code role} as claims.
     */
    public String generateToken(UserPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",        principal.getAuthorities().iterator().next().getAuthority());
        claims.put("warehouseId", principal.getWarehouseId());
        claims.put("type",        "USER");
        return createToken(claims, principal.getUsername());
    }

    /**
     * Generates a JWT for a supplier.
     * No warehouseId — suppliers are global.
     */
    public String generateToken(SupplierPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_SUPPLIER");
        claims.put("type", "SUPPLIER");
        return createToken(claims, principal.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }

    private Boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }
}
