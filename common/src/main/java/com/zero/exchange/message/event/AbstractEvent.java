package com.zero.exchange.message.event;

import com.zero.exchange.message.AbstractMessage;
import org.springframework.lang.Nullable;

public class AbstractEvent extends AbstractMessage {
    /**
     * Message after sequence id
     * */
    public long sequenceId;

    /**
     * Previous message sequence id
     * */
    public long previousId;

    /**
     * Unique id
     * */
    @Nullable
    public long uniqueId;
}
