package com.zero.exchange.service;

import com.zero.exchange.message.event.AbstractEvent;

public interface SendEventService {

    /**
     * 发送 MQ 消息
     *
     * @param event 发送的事件
     * */
    void sendMessage(AbstractEvent event);
}
