package com.exchange.common.model.trade;

import com.exchange.common.enums.Direction;
import com.exchange.common.enums.MatchType;
import com.exchange.common.model.support.EntitySupport;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * 撮合结果，只读
 * */
@Entity
@Table(name = "match_details", uniqueConstraints = @UniqueConstraint(name = "UNI_OID_COID", columnNames = {"orderId",
        "countOrderId"}), indexes = @Index(name = "IDX_OID_CT", columnList = "orderId, createAt"))
public class MatchDetailEntity implements EntitySupport, Comparable<MatchDetailEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public long id;

    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long orderId;

    @Column(nullable = false, updatable = false)
    public Long counterOrderId;

    @Column(nullable = false, updatable = false)
    public Long account;

    @Column(nullable = false, updatable = false)
    public Long counterAccount;

    @Column(nullable = false, updatable = false)
    public MatchType type;

    @Column(nullable = false, updatable = false)
    public Direction direction;

    @Column(nullable = false, updatable = false)
    public BigDecimal price;

    @Column(nullable = false, updatable = false)
    public BigDecimal quality;

    @Column(nullable = false, updatable = false)
    public long createAt;

    /**
     * 按OrderId, CounterOrderId排序
     */
    @Override
    public int compareTo(@NotNull MatchDetailEntity o) {
        int cmp = Long.compare(this.orderId.longValue(), o.orderId.longValue());
        if (cmp == 0) {
            cmp = Long.compare(this.counterOrderId.longValue(), o.counterOrderId.longValue());
        }
        return cmp;
    }
}
