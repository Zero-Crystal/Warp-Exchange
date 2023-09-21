package com.exchange.trade.engin.trade;

import com.exchange.common.message.event.AbstractEvent;
import com.exchange.common.message.event.OrderCancelEvent;
import com.exchange.common.message.event.OrderRequestEvent;
import com.exchange.common.message.event.TransferEvent;

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

    /**
     * 新建订单
     * */
    void createOrder(OrderRequestEvent event);

    /**
     * 取消订单
     * */
    void cancelOrder(OrderCancelEvent event);

    /**
     * 订单交易
     * */
    boolean transfer(TransferEvent event);
}
