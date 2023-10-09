package com.zero.exchange.match.service;

import com.zero.exchange.model.OrderBookBean;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.match.model.MatchResult;
import com.zero.exchange.match.model.OrderBook;

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

    void debug();
}
