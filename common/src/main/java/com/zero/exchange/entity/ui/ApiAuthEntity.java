package com.zero.exchange.entity.ui;

import com.zero.exchange.entity.support.EntitySupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_key_auths")
public class ApiAuthEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String apiKey;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String apiSecret;

    /**
     * apiSecret 过期时间
     * */
    @Column(nullable = false, updatable = false)
    public long expiresAt;

    @Override
    public String toString() {
        return "ApiAuthEntity [" + "userId: " + userId + ", apiKey: '" + apiKey +
                ", apiSecret: **********, expiresAt: ]" + expiresAt;
    }
}
