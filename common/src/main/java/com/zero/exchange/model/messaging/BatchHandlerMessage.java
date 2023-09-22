package com.zero.exchange.model.messaging;

import com.zero.exchange.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface BatchHandlerMessage<T extends AbstractMessage> {

    void processMessage(List<T> messages);
}
