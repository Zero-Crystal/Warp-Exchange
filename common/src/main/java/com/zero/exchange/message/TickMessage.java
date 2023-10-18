package com.zero.exchange.message;

import com.zero.exchange.entity.quotation.TickEntity;

import java.util.List;

public class TickMessage extends AbstractMessage{

    public long sequenceId;

    public List<TickEntity> ticks;

    @Override
    public String toString() {
        return "TickMessage [" +
                "refId='" + refId + '\'' +
                ", createAt=" + createAt +
                ", sequenceId=" + sequenceId +
                ']';
    }
}
