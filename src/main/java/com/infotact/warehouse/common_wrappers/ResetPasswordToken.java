package com.infotact.warehouse.common_wrappers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

/**
 * Redis-backed entity to store password reset tokens.
 * <p>
 * TTL is set to 900 seconds (15 minutes). After this window,
 * the token is automatically evicted from Redis.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "ResetPasswordToken", timeToLive = 900)
public class ResetPasswordToken implements Serializable {

    /**
     * The unique secure token generated during the forgot-password flow.
     */
    @Id
    private String token;

    /**
     * The email address associated with this reset request.
     */
    private String email;
}