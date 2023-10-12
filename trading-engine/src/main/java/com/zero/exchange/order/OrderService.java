package com.zero.exchange.order;

import com.zero.exchange.enums.Direction;
import com.zero.exchange.entity.trade.OrderEntity;

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
     * @param userId 用户Id
     * @param price 订单价格
     * @param direction 交易方向
     * @param quantity 订单数量
     * */
    OrderEntity createOrder(long createTime, Long orderId, long sequenceId, Long userId, BigDecimal price, Direction direction, BigDecimal quantity);

    /**
     * 删除订单
     * @param userId 用户Id
     * @param orderId 订单Id
     * */
    void removeOrder(Long userId, Long orderId);

    /**
     * 查询活跃的订单
     * @return ConcurrentMap<Long, OrderEntity>
     * */
    ConcurrentMap<Long, OrderEntity> getActiveOrders();

    /**
     * 获取全部用户订单
     * @return ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>>
     * */
    ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> getUserOrders();

    /**
     * 查询订单
     * @param orderId
     * @return OrderEntity
     * */
    OrderEntity getOrderByOrderId(Long orderId);

    /**
     * 查询订单
     * @param userId
     * @return ConcurrentMap<Long, OrderEntity>
     * */
    ConcurrentMap<Long, OrderEntity> getOrderMapByUserId(Long userId);

    void debug();
}
