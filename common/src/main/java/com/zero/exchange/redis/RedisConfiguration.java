package com.zero.exchange.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("spring.redis")
@Data
public class RedisConfiguration {
    private String host;
    private int port;
    private String password;
    private int database;
}
