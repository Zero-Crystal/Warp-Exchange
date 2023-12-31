package com.zero.exchange.order;

import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.asset.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrderServiceImpl implements OrderService {

    private final AssetService assetService;

    /**
     * 跟踪所有活动的订单， OrderId -> OrderEntity
     * */
    private final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    /**
     * 跟踪用户活动的订单，userId -> ConcurrentMap<OrderId, OrderEntity>
     * */
    private final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    public OrderServiceImpl(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }

    @Override
    public OrderEntity createOrder(long createTime, Long orderId, long sequenceId, Long userId, BigDecimal price, Direction direction, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                if (!assetService.assetFreeze(userId, AssetType.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                if (!assetService.assetFreeze(userId, AssetType.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalStateException("未知交易方向：" + direction);
        }
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.id = orderId;
        orderEntity.sequenceId = sequenceId;
        orderEntity.userId = userId;
        orderEntity.price = price;
        orderEntity.direction = direction;
        orderEntity.quantity = orderEntity.unfilledQuantity = quantity;
        orderEntity.createdAt = orderEntity.updateAt = createTime;
        // 添加到活动的订单
        activeOrders.put(orderEntity.id, orderEntity);
        // 添加到用户所有活动的订单
        ConcurrentMap<Long, OrderEntity> userOrders = this.userOrders.get(userId);
        if (userOrders == null) {
            userOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId, userOrders);
        }
        userOrders.put(orderEntity.id, orderEntity);
        return orderEntity;
    }

    @Override
    public void removeOrder(Long userId, Long orderId) {
        // 1.移除活动订单
        OrderEntity orderRemoved = activeOrders.remove(orderId);
        if (orderRemoved == null) {
            throw new IllegalArgumentException("未找到当前订单, orderId=" + orderId);
        }
        // 2.移除用户活动的订单
        ConcurrentMap<Long, OrderEntity> orderMap = userOrders.get(userId);
        if (orderMap == null) {
            throw new IllegalArgumentException("未找到该用户[" + userId + "]");
        }
        if (orderMap.remove(orderId) == null) {
            throw new IllegalArgumentException("未找到用户[" + userId + "]的订单, orderId=" + orderId);
        }
    }

    @Override
    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return activeOrders;
    }

    @Override
    public ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> getUserOrders() {
        return userOrders;
    }

    @Override
    public OrderEntity getOrderByOrderId(Long orderId) {
        return activeOrders.get(orderId);
    }

    @Override
    public ConcurrentMap<Long, OrderEntity> getOrderMapByUserId(Long userId) {
        return userOrders.get(userId);
    }

    @Override
    public void debug() {
        System.out.println("----------------------------order----------------------------");
        List<OrderEntity> orderEntities = new ArrayList<>(activeOrders.values());
        Collections.sort(orderEntities);
        for (OrderEntity order : orderEntities) {
            System.out.println(order.toString());
        }
    }
}
