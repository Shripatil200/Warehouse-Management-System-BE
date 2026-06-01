package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed implementation of {@link TokenBlacklistService}.
 * <p>
 * Each revoked token is stored as a Redis key with TTL equal to the token's
 * remaining lifetime so entries are automatically garbage-collected.  Redis
 * is already a required dependency (used for caching), so no new infrastructure
 * is needed.
 * </p>
 *
 * <p><b>Key format:</b> {@code jwt:blacklist:<token>}</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    /** Prefix keeps blacklist entries isolated from cache keys. */
    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final java.util.Map<String, Long> localBlacklist = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void blacklist(String token, long expiryMs) {
        long remainingMs = expiryMs - System.currentTimeMillis();
        if (remainingMs <= 0) {
            // Token is already expired — nothing to blacklist
            log.debug("Token already expired; skipping blacklist.");
            return;
        }
        try {
            String key = PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, "1", Duration.ofMillis(remainingMs));
            log.info("Token blacklisted in Redis for {}ms", remainingMs);
            return;
        } catch (Exception e) {
            log.warn("Redis is unavailable for blacklisting, falling back to in-memory storage: {}", e.getMessage());
        }

        localBlacklist.put(token, expiryMs);
        log.info("Token blacklisted in-memory for {}ms", remainingMs);
        cleanExpiredLocalTokens();
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(PREFIX + token));
        } catch (Exception e) {
            log.warn("Redis is unavailable for blacklist check, falling back to in-memory check: {}", e.getMessage());
        }

        Long expiry = localBlacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            localBlacklist.remove(token);
            return false;
        }
        return true;
    }

    private void cleanExpiredLocalTokens() {
        long now = System.currentTimeMillis();
        localBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
