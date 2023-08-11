package com.exchange.trade.engin.clearing;

import com.exchange.common.module.trade.OrderEntity;
import com.exchange.trade.engin.match.model.MatchResult;

public interface ClearingService {

    /**
     * 清算订单撮合结果
     * @param matchResult
     * @return boolean 清算结果
     * */
    void clearingMatchResult(MatchResult matchResult);

    /**
     * 清算取消
     * @param order 带取消清算的订单
     * */
    void clearingCancel(OrderEntity order);
}
