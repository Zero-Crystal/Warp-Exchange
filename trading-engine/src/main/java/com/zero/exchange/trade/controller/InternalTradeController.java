package com.zero.exchange.trade.controller;

import com.zero.exchange.api.ApiResult;
import com.zero.exchange.asset.model.Asset;
import com.zero.exchange.asset.service.AssetService;
import com.zero.exchange.enums.AssetType;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.order.OrderService;
import com.zero.exchange.support.LoggerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/internal")
public class InternalTradeController extends LoggerSupport implements InternalTradeApi {

    @Autowired
    private AssetService assetService;

    @Autowired
    private OrderService orderService;

    @Override
    @GetMapping("/{userId}/assets")
    public ApiResult getAssets(@PathVariable Long userId) {
        Map<AssetType, Asset> assetMap = assetService.getAssets(userId);
        if (assetMap.isEmpty()) {
            log.info("未获取到用户[{}]的资产信息", userId);
            return ApiResult.success(Map.of());
        }
        log.info("get user[{}] asset[{}]", userId, assetMap);
        return ApiResult.success(assetMap);
    }

    @Override
    @GetMapping("/{userId}/orders")
    public ApiResult getOrders(@PathVariable Long userId) {
        ConcurrentMap<Long, OrderEntity> userOrders = orderService.getOrderMapByUserId(userId);
        if (userOrders == null || userOrders.isEmpty()) {
            log.info("未获取到用户[{}]的订单", userId);
            return ApiResult.success(List.of());
        }
        List<OrderEntity> orderList = new ArrayList<>(userOrders.size());
        for (OrderEntity order : userOrders.values()) {
            OrderEntity copy = null;
            while (copy == null) {
                copy = order.copy();
            }
            orderList.add(order);
        }
        log.info("获取用户[{}]的订单信息：{}", userId, orderList);
        return ApiResult.success(orderList);
    }

    @Override
    @GetMapping("/{userId}/orders/{orderId}")
    public ApiResult getOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        OrderEntity userOrder = orderService.getOrderByOrderId(orderId);
        if (userOrder == null || userOrder.userId.longValue() != userId.longValue()) {
            log.info("未获取到用户[{}]的订单信息", userId);
            return ApiResult.success(null);
        }
        log.info("获取用户[{}]的订单信息：{}", userId, userOrder);
        return ApiResult.success(userOrder);
    }
}
