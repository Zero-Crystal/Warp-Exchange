package com.zero.exchange.entity.trade;

import com.zero.exchange.entity.support.EntitySupport;
import jakarta.persistence.*;

/**
 * 消息订阅实体
 *
 * @createAt 2023/09/05
 * */
@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(name = "UNI_PREV_ID", columnNames = "previousId"))
public class EventEntity implements EntitySupport {

    /**
     * 消息定序 id
     * */
    @Id
    @Column(nullable = false, updatable = false)
    public Long sequenceId;

    /**
     * 消息数据；格式：JSON
     * */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_10000)
    public String data;

    /**
     * 上一条消息的定序id. 首条消息的id为 0.
     * */
    @Column(nullable = false, updatable = false)
    public Long previousId;

    /**
     * 消息创建时间
     * */
    @Column(nullable = false, updatable = false)
    public long createAt;

    @Override
    public String toString() {
        return "EventEntity [" + "sequenceId: " + sequenceId + ", data: '" + data +
                ", previousId: " + previousId + ", createAt: " + createAt + "]";
    }
}
