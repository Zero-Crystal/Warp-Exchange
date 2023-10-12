package com.zero.exchange.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "exchange.config")
public class ExchangeConfiguration {

    private int orderBookDepth = 50;

    private boolean isDebugMode = false;

    private String hmacKey;
}
