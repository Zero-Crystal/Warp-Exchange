package com.zero.exchange.model;

import com.zero.exchange.util.HashUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record AuthToken(Long userId, long expiresAt) {

    /**
     * 是否过期
     * */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt();
    }

    public boolean isAboutToExpire() {
        return expiresAt() - System.currentTimeMillis() < 1800_000;
    }

    public AuthToken refresh() {
        return new AuthToken(userId(), System.currentTimeMillis() + 3600_000);
    }

    /**
     * hash = hmacSha256(userId : expiresAt, hmacKey)
     *
     * secureString = userId : expiresAt : hash
     */
    public String toSecureString(String hmcKey) {
        String payLoad = userId() + ":" + expiresAt();
        String hash = HashUtil.hmacSha256(payLoad, hmcKey);
        String token = payLoad + ":" + hash;
        return Base64.getUrlEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public static AuthToken fromSecureString(String b64token, String hmacKey) {
        String token = new String(Base64.getUrlDecoder().decode(b64token), StandardCharsets.UTF_8);
        String[] secureStrings = token.split(":");
        if (secureStrings.length != 3) {
            throw new IllegalArgumentException("Invalid token: " + b64token);
        }
        String userId = secureStrings[0];
        String expiresAt = secureStrings[1];
        String hash = secureStrings[2];
        if (!hash.equals(HashUtil.hmacSha256(userId + ":" + expiresAt, hmacKey))) {
            throw new IllegalArgumentException("Invalid token: " + b64token);
        }
        return new AuthToken(Long.parseLong(userId), Long.parseLong(expiresAt));
    }
}
