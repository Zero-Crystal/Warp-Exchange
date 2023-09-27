package com.zero.exchange.model.trade;

import com.zero.exchange.model.support.EntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "unique_event")
public class UniqueEventEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false, length = VAR_CHAR_50)
    public String uniqueId;

    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "[" + "uniqueId: '" + uniqueId
                + ", sequenceId: " + sequenceId +
                ", createdAt: " + createdAt + "]";
    }
}
