package com.zero.exchange.model.trade;

import com.zero.exchange.model.support.EntitySupport;
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
     * id
     * */
    @Id
    @Column(nullable = false, updatable = false)
    public Integer id;

    /**
     * 消息定序 id
     * */
    @Column(nullable = false, updatable = false)
    public Long sequenceId;

    /**
     * 消息创建时间
     * */
    @Column(nullable = false, updatable = false)
    public long createAt;

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

    @Override
    public String toString() {
        return "EventEntity [" + "id: " + id + ", sequencerId: " + sequenceId + ", createAt: " + createAt +
                ", data: " + data + ", previousId: " + previousId + ']';
    }
}
