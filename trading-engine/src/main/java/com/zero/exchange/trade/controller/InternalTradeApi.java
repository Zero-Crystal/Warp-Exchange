package com.zero.exchange.trade.controller;

import com.zero.exchange.api.ApiResult;

public interface InternalTradeApi {

    /**
     * 获取用户资产
     *
     * @param userId 用户id
     * @return ApiResult
     * */
    ApiResult getAssets(Long userId);

    /**
     * 获取用户订单
     *
     * @param userId 用户id
     * @return ApiResult
     * */
    ApiResult getOrders(Long userId);

    /**
     * 获取用户订单
     *
     * @param userId 用户id
     * @param orderId 订单id
     * @return ApiResult
     * */
    ApiResult getOrder(Long userId, Long orderId);
}
