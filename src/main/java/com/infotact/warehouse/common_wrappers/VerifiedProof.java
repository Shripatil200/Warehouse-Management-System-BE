package com.infotact.warehouse.common_wrappers;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "VerifiedProof", timeToLive = 900) // 15 minutes
public class VerifiedProof implements Serializable {
    @Id
    private String token; // The UUID (eml-xxx or cnt-xxx)
    private String identifier; // The email/phone it belongs to
}