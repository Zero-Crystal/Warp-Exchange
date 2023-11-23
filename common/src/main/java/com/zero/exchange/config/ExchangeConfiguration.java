package com.zero.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "exchange.config")
public class ExchangeConfiguration {

    private int orderBookDepth = 50;

    private boolean isDebugMode = false;

    private String hmacKey;

    private Duration sessionTimeout;

    private boolean backupEnable;

    private String backupPath;

    private ApiEndpoints apiEndpoints;

    public int getOrderBookDepth() {
        return orderBookDepth;
    }

    public void setOrderBookDepth(int orderBookDepth) {
        this.orderBookDepth = orderBookDepth;
    }

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public void setDebugMode(boolean debugMode) {
        isDebugMode = debugMode;
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public boolean isBackupEnable() {
        return backupEnable;
    }

    public void setBackupEnable(boolean backupEnable) {
        this.backupEnable = backupEnable;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public ApiEndpoints getApiEndpoints() {
        return apiEndpoints;
    }

    public void setApiEndpoints(ApiEndpoints apiEndpoints) {
        this.apiEndpoints = apiEndpoints;
    }

    public static class ApiEndpoints {

        private String tradeApi;

        private String tradeEnginApi;

        public String getTradeApi() {
            return tradeApi;
        }

        public void setTradeApi(String tradeApi) {
            this.tradeApi = tradeApi;
        }

        public String getTradeEnginApi() {
            return tradeEnginApi;
        }

        public void setTradeEnginApi(String tradeEnginApi) {
            this.tradeEnginApi = tradeEnginApi;
        }
    }
}
