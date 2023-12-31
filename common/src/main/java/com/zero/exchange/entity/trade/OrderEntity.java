package com.zero.exchange.entity.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.enums.OrderStatus;
import com.zero.exchange.entity.support.EntitySupport;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity> {

    /**
     * 订单id
     * */
    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * 定序id
     * */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * 用户id
     * */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 价格
     * */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    /**
     * 方向
     * */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    /**
     * 状态
     * */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public OrderStatus status;

    /**
     * 订单数量
     * */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * 待成交数量
     * */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal unfilledQuantity;

    /**
     * 创建时间
     * */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    /**
     * 更新时间
     * */
    @Column(nullable = false, updatable = false)
    public long updateAt;

    private int version;

    @Transient
    @JsonIgnore
    public int getVersion() {
        return version;
    }

    @Override
    public int compareTo(@NotNull OrderEntity o) {
        return Long.compare(this.id.longValue(), o.id.longValue());
    }

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.version++;
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updateAt = updatedAt;
        this.version++;
    }

    @Nullable
    public OrderEntity copy() {
        OrderEntity entity = new OrderEntity();
        int version = this.version;
        entity.id = this.id;
        entity.sequenceId = this.sequenceId;
        entity.userId = this.userId;
        entity.price = this.price;
        entity.direction = this.direction;
        entity.status = this.status;
        entity.quantity = this.quantity;
        entity.unfilledQuantity = this.unfilledQuantity;
        entity.createdAt = this.createdAt;
        entity.updateAt = this.updateAt;
        if (version != this.version) {
            return null;
        }
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OrderEntity) {
            OrderEntity e = (OrderEntity) o;
            return this.id.longValue() == e.id.longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return id + " [sequenceId: " + sequenceId +
                ", userId: " + userId +
                ", price: " + price +
                ", direction: " + direction +
                ", status: " + status +
                ", quantity: " + quantity +
                ", unfilledQuantity: " + unfilledQuantity +
                ", createdAt: " + createdAt +
                ", updateAt: " + updateAt + "]";
    }
}
