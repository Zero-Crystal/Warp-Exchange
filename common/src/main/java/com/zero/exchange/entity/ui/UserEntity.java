package com.zero.exchange.entity.ui;

import com.zero.exchange.enums.UserType;
import com.zero.exchange.entity.support.EntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity implements EntitySupport {

    /**
     * Primary Key: auto-increment
     * */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public Long id;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public UserType type;

    /**
     * Create time
     * */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "UserEntity [" + "id：" + id + ", type：" + type + ", createdAt：" + createdAt + ']';
    }
}
