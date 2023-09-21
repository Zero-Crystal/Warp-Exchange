package com.exchange.trade.engin.match.service;

import com.exchange.common.bean.OrderBookBean;
import com.exchange.common.enums.Direction;
import com.exchange.common.model.trade.OrderEntity;
import com.exchange.trade.engin.match.model.MatchResult;
import com.exchange.trade.engin.match.model.OrderBook;

import java.math.BigDecimal;

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

    /**
     * 获取订单簿
     *
     * @param direction 交易方向
     * @return OrderBook
     * */
    OrderBook getOrderBook(Direction direction);

    /**
     * 获取市场价格
     * */
    BigDecimal getMarketPrice();
}
