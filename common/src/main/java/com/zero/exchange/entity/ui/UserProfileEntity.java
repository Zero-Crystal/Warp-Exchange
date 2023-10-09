package com.zero.exchange.entity.ui;

import com.zero.exchange.entity.support.EntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "user_profile", uniqueConstraints = @UniqueConstraint(name = "UNI_EMAIL", columnNames = {"email"}))
public class UserProfileEntity implements EntitySupport {

    /**
     * 关联 users 表
     * */
    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 用户注册邮箱
     * */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_100)
    public String email;

    /**
     * 用户名称
     * */
    @Column(nullable = false, length = VAR_CHAR_100)
    public String name;

    /**
     * 用户信息创建时间
     * */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    /**
     * 用户信息更新时间
     * */
    @Column(nullable = false)
    public long updatedAt;

    @Override
    public String toString() {
        return "UserProfileEntity [" + "userId：" + userId + ", email：'" + email + '\'' +
                ", name：'" + name + '\'' + ", createdAt：" + createdAt + ", updatedAt：" + updatedAt + ']';
    }
}
