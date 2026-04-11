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
@RedisHash(value = "OtpToken", timeToLive = 300) // 5 minutes
public class OtpToken implements Serializable {
    @Id
    private String identifier; // email or phone number
    private String otpCode;
}
