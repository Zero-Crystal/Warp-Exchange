package com.exchange.trade.engin.order;

import com.exchange.common.enums.Direction;
import com.exchange.common.model.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentMap;

/**
 * 订单服务
 * @author zero
 * @createTime 2023/07/26
 * */
public interface OrderService {

    /**
     * 创建订单
     * @param sequenceId 定序ID
     * @param accountId 账户ID
     * @param price 订单价格
     * @param direction 交易方向
     * @param quantity 订单数量
     * */
    OrderEntity createOrder(long createTime, Long orderId, long sequenceId, Long accountId, BigDecimal price, Direction direction, BigDecimal quantity);

    /**
     * 删除订单
     * @param accountId 账户Id
     * @param orderId 订单Id
     * */
    void removeOrder(Long accountId, Long orderId);

    /**
     * 查询活跃的订单
     * @return ConcurrentMap<Long, OrderEntity>
     * */
    ConcurrentMap<Long, OrderEntity> getActiveOrders();

    /**
     * 查询订单
     * @param orderId
     * @return OrderEntity
     * */
    OrderEntity getOrderByOrderId(Long orderId);

    /**
     * 查询订单
     * @param accountId
     * @return ConcurrentMap<Long, OrderEntity>
     * */
    ConcurrentMap<Long, OrderEntity> getOrderMapByAccountId(Long accountId);
}
