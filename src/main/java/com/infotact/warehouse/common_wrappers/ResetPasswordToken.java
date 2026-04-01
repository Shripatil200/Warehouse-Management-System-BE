package com.infotact.warehouse.common_wrappers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id; // <-- USE THIS ONE
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "ResetPasswordToken", timeToLive = 900) // 15 mins
public class ResetPasswordToken implements Serializable {

    @Id
    private String token;

    private String email;

}