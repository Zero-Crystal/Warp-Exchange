package com.zero.exchange.message.event;

import com.zero.exchange.message.AbstractMessage;
import org.springframework.lang.Nullable;

public class AbstractEvent extends AbstractMessage {
    /**
     * 定序后的定序
     * */
    public long sequenceId;

    /**
     * 上一条消息的定序id
     * */
    public long previousId;

    /**
     * 可选的全局唯一标识
     * */
    @Nullable
    public String uniqueId;
}
