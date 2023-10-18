package com.zero.exchange.api;

import com.zero.exchange.model.OrderVO;
import com.zero.exchange.model.UserSignUpVO;
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
     * 注册账号
     *
     * @param userSignUpVO
     * @return ApiResult
     * */
    ApiResult signUp(UserSignUpVO userSignUpVO);

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

    /**
     * 获取按天计算的 bar
     *
     * @return ApiResult
     * */
    ApiResult getDayBar();

    /**
     * 获取按小时计算的 bar
     *
     * @return ApiResult
     * */
    ApiResult getHourBar();

    /**
     * 获取按分钟计算的 bar
     *
     * @return ApiResult
     * */
    ApiResult getMinBar();

    /**
     * 获取按秒计算的 bar
     *
     * @return ApiResult
     * */
    ApiResult getSecondBar();
}
