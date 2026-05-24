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

    @Override
    public void blacklist(String token, long expiryMs) {
        long remainingMs = expiryMs - System.currentTimeMillis();
        if (remainingMs <= 0) {
            // Token is already expired — nothing to blacklist
            log.debug("Token already expired; skipping blacklist.");
            return;
        }
        String key = PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, "1", Duration.ofMillis(remainingMs));
        log.info("Token blacklisted for {}ms", remainingMs);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(PREFIX + token));
    }
}
