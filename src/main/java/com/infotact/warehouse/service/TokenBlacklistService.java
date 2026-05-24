package com.infotact.warehouse.service;

/**
 * Service for managing a Redis-backed JWT blacklist.
 * <p>
 * When a user logs out or an admin force-revokes a token, the token's JTI
 * (or the raw token string) is stored in Redis with a TTL matching the token's
 * remaining lifetime.  The {@link com.infotact.warehouse.config.JWT.JwtFilter}
 * checks this list on every request, immediately honouring password changes
 * and role downgrades without waiting up to 10 hours for natural expiry.
 * </p>
 */
public interface TokenBlacklistService {

    /**
     * Blacklists a raw JWT string until its natural expiry.
     * Subsequent requests presenting this token will receive 401.
     *
     * @param token      the raw JWT (Bearer value, without "Bearer " prefix)
     * @param expiryMs   absolute expiry epoch-millis from the token's {@code exp} claim
     */
    void blacklist(String token, long expiryMs);

    /**
     * Returns {@code true} if the given token has been explicitly revoked.
     *
     * @param token the raw JWT string
     */
    boolean isBlacklisted(String token);
}
