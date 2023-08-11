package com.exchange.trade.engin.match.service;

import com.exchange.common.bean.OrderBookBean;
import com.exchange.common.module.trade.OrderEntity;
import com.exchange.trade.engin.match.model.MatchResult;
import com.exchange.trade.engin.match.model.OrderBook;

/**
 * 撮合引擎服务
 * */
public interface MatchService {

    /**
     * 订单匹配
     * @param sequenceId 定序id
     * @param order 待匹配订单
     * @return MatchResult
     * */
    MatchResult matchOrder(long sequenceId, OrderEntity order);

    /**
     * 订单撮合
     * @param sequenceId 定序id
     * @param takerOrder 输入订单
     * @param makerBook 尝试匹配成交的OrderBook
     * @param anotherBook 未能完全成交后挂单的OrderBook
     * @return MatchResult 交易结果
     * */
    MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook);

    /**
     * 取消交易
     * @param ts
     * @param order
     * */
    void cancel(long ts, OrderEntity order);

    /**
     * 获取订单簿
     * @param maxDepth
     * @return OrderBook
     * */
    OrderBookBean getOrderBook(int maxDepth);
}
