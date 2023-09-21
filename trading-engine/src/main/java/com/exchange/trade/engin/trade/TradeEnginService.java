package com.exchange.trade.engin.trade;

import com.exchange.common.message.event.AbstractEvent;

import java.util.List;

public interface TradeEnginService {

    /**
     * 订阅消息
     * */
    void receiveMessage(List<AbstractEvent> receiveMessages);

    /**
     * 处理订阅事件
     * */
    void processEvent(AbstractEvent abstractEvent);
}
