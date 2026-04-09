package com.infotact.warehouse.config.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility service for managing JSON Web Tokens (JWT).
 * <p>
 * This class provides methods to generate, parse, and validate JWTs using
 * the HMAC SHA-256 signing algorithm.
 * </p>
 */
@Service
public class JwtUtil {

    @Value("${app.jwt.secret:mySuperSecretKeyForJwtGeneration12345}")
    private String secret;

    /**
     * Extracts the subject (username/email) from the token.
     */
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the token.
     */
    public Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT using the secret key to retrieve all stored claims.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Creates a new JWT with the user's role and email.
     * * @param username The user's email address.
     * @param role The user's assigned Role (ADMIN/MANAGER).
     * @return A signed JWT string.
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 Hours
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates if the token belongs to the user and has not expired.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}