package com.zero.exchange.api;

import com.zero.exchange.model.OrderVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

public interface TradeApi {

    /**
     * 获取时间戳
     *
     * @return ApiResult
     * */
    ApiResult timestamp();

    /**
     * 获取用户资产
     *
     * @return ApiResult
     * */
    ApiResult getAssets() throws IOException;

    /**
     * 获取用户订单
     *
     * @param orderId
     * @return ApiResult
     * */
    ApiResult getOpenOrder(Long orderId) throws IOException;

    /**
     * 获取用户订单
     *
     * @return ApiResult
     * */
    ApiResult getOpenOrders() throws IOException;

    /**
     * 获取订单簿
     *
     * @return ApiResult
     * */
    ApiResult getOrderBook();

    /**
     * 获取历史订单
     *
     * @param maxResult
     * @return ApiResult
     * */
    ApiResult getHistoryOrder(int maxResult);

    /**
     * 获取历史订单详情
     *
     * @param orderId
     * @return ApiResult
     * */
    ApiResult getHistoryOrderMatched(Long orderId) throws Exception;

    /**
     * 新建订单
     *
     * @param orderVO
     * @return ApiResult
     * */
    DeferredResult<ResponseEntity<String>> createOrder(OrderVO orderVO) throws IOException;

    /**
     * 取消订单
     *
     * @param orderId
     * @return ApiResult
     * */
    DeferredResult<ResponseEntity<String>> cancelOrder(Long orderId) throws IOException;

    /**
     * 获取近期 tick
     *
     * @return ApiResult
     * */
    ApiResult getRecentTicks();

    ApiResult getDayBar();

    ApiResult getHourBar();

    ApiResult getMinBar();

    ApiResult getSecondBar();
}
