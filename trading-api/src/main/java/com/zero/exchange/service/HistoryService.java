package com.zero.exchange.service;

import com.zero.exchange.model.OrderMatchedDetailVO;
import com.zero.exchange.entity.trade.OrderEntity;

import java.util.List;

public interface HistoryService {

    /**
     * 查询历史订单
     *
     * @param userId
     * @param maxResult
     * @return List<OrderEntity>
     * */
    List<OrderEntity> getHistoryOrders(Long userId, int maxResult);

    /**
     * 查询历史订单
     *
     * @param userId
     * @return OrderEntity
     * */
    OrderEntity getHistoryOrder(Long userId, Long orderId);

    /**
     * 查询历史订单细节
     *
     * @param orderId
     * @return MatchDetailEntity
     * */
    List<OrderMatchedDetailVO> getHistoryOrderDetail(Long orderId);
}
