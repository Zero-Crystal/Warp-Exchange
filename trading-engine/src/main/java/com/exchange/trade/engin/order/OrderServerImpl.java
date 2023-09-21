package com.exchange.trade.engin.order;

import com.exchange.common.enums.AssetType;
import com.exchange.common.enums.Direction;
import com.exchange.common.model.trade.OrderEntity;
import com.exchange.trade.engin.asset.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrderServerImpl implements OrderService {

    private final AssetService assetService;

    /**
     * 跟踪所有活动的订单， OrderId -> OrderEntity
     * */
    private final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    /**
     * 跟踪用户活动的订单，accountId -> ConcurrentMap<OrderId, OrderEntity>
     * */
    private final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    public OrderServerImpl(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }

    @Override
    public OrderEntity createOrder(long createTime, Long orderId, long sequenceId, Long accountId, BigDecimal price, Direction direction, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                if (!assetService.assetFreeze(accountId, AssetType.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                if (!assetService.assetFreeze(accountId, AssetType.BTC, price.multiply(quantity))) {
                    return null;
                }
            }
            default -> throw new IllegalStateException("未知交易方向：" + direction);
        }
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.id = orderId;
        orderEntity.sequenceId = sequenceId;
        orderEntity.accountId = accountId;
        orderEntity.price = price;
        orderEntity.direction = direction;
        orderEntity.quantity = orderEntity.unfilledQuantity = quantity;
        orderEntity.createdAt = orderEntity.updateAt = createTime;
        // 添加到活动的订单
        activeOrders.put(orderId, orderEntity);
        // 添加到用户所有活动的订单
        ConcurrentMap<Long, OrderEntity> orderMap = userOrders.get(accountId);
        if (orderMap == null) {
            orderMap = new ConcurrentHashMap<>();
            userOrders.put(accountId, orderMap);
        }
        orderMap.put(orderId, orderEntity);
        return orderEntity;
    }

    @Override
    public void removeOrder(Long accountId, Long orderId) {
        // 1.移除活动订单
        OrderEntity orderRemoved = activeOrders.remove(orderId);
        if (orderRemoved == null) {
            throw new IllegalArgumentException("未找到当前订单, orderId=" + orderId);
        }
        // 2.移除用户活动的订单
        ConcurrentMap<Long, OrderEntity> orderMap = userOrders.get(accountId);
        if (orderMap == null) {
            throw new IllegalArgumentException("未找到该用户[" + accountId + "]");
        }
        if (orderMap.remove(orderId) == null) {
            throw new IllegalArgumentException("未找到用户[" + accountId + "]的订单, orderId=" + orderId);
        }
    }

    @Override
    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return activeOrders;
    }

    @Override
    public OrderEntity getOrderByOrderId(Long orderId) {
        return activeOrders.get(orderId);
    }

    @Override
    public ConcurrentMap<Long, OrderEntity> getOrderMapByAccountId(Long accountId) {
        return userOrders.get(accountId);
    }
}
