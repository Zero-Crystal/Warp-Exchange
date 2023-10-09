package com.zero.exchange.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zero.exchange.api.ApiResult;
import com.zero.exchange.api.TradeApi;
import com.zero.exchange.api.redis.ApiError;
import com.zero.exchange.api.redis.ApiException;
import com.zero.exchange.message.ApiResultMessage;
import com.zero.exchange.message.event.OrderCancelEvent;
import com.zero.exchange.message.event.OrderRequestEvent;
import com.zero.exchange.model.OrderBookBean;
import com.zero.exchange.context.UserContext;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.model.OrderVO;
import com.zero.exchange.redis.RedisCache;
import com.zero.exchange.redis.RedisService;
import com.zero.exchange.service.HistoryService;
import com.zero.exchange.service.SendEventService;
import com.zero.exchange.service.TradeEnginApiService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.util.IdUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@RestController
@RequestMapping("/api")
public class TradeApiController extends LoggerSupport implements TradeApi {

    @Autowired
    private TradeEnginApiService tradeEnginApiService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, DeferredResult<ResponseEntity<String>>> deferredResultMap = new HashMap<>();

    private Long asyncTimeout = Long.valueOf(500);

    private String timeoutJson;

    @PostConstruct
    public void init() {
        redisService.subscribe(RedisCache.Topic.TRADE_API_RESULT, this::onApiResultMessage);
    }

    @Override
    @GetMapping(value = "/timestamp", produces = "application/json")
    public ApiResult timestamp() {
        return ApiResult.success(Map.of("timestamp", System.currentTimeMillis()));
    }

    @Override
    @GetMapping(value = "/assets", produces = "application/json")
    public ApiResult getAssets() throws IOException {
        String url = "/internal/" + UserContext.getRequiredUserId() + "/assets";
        return objectMapper.readValue(tradeEnginApiService.get(url), ApiResult.class);
    }

    @Override
    @GetMapping(value = "/orders/{orderId}", produces = "application/json")
    public ApiResult getOpenOrder(@PathVariable Long orderId) throws IOException {
        String url = "/internal/" + UserContext.getRequiredUserId() + "/orders";
        return objectMapper.readValue(tradeEnginApiService.get(url), ApiResult.class);
    }

    @Override
    @GetMapping(value = "/orders", produces = "application/json")
    public ApiResult getOpenOrders() throws IOException {
        String url = "/internal/" + UserContext.getRequiredUserId() + "/orders";
        return objectMapper.readValue(tradeEnginApiService.get(url), ApiResult.class);
    }

    @Override
    @GetMapping(value = "/orderBook", produces = "application/json")
    public ApiResult getOrderBook() {
        String data = redisService.get(RedisCache.Key.ORDER_BOOK);
        return ApiResult.success(data == null ? OrderBookBean.EMPTY : data);
    }

    @Override
    @GetMapping(value = "/history/orders", produces = "application/json")
    public ApiResult getHistoryOrder(@RequestParam(name = "maxResult", defaultValue = "50", required = false) int maxResult) {
        log.info("获取用户[{}]的历史订单，返回结果数量[{}]", UserContext.getRequiredUserId(), maxResult);
        if (maxResult < 1 || maxResult > 1000) {
            return ApiResult.failure("参数不合法");
        }
        return ApiResult.success(historyService.getHistoryOrders(UserContext.getRequiredUserId(), maxResult));
    }

    @Override
    @GetMapping(value = "/history/orders/{orderId}", produces = "application/json")
    public ApiResult getHistoryOrderMatched(@PathVariable Long orderId) throws Exception {
        log.info("获取历史订单[{}]的匹配详情", orderId);
        final Long userId = UserContext.getRequiredUserId();
        String orderString = tradeEnginApiService.get("/internal/" + userId + "/orders/" + orderId);
        ApiResult result = objectMapper.readValue(orderString, ApiResult.class);
        if (result.isSuccess()) {
            OrderEntity order = historyService.getHistoryOrder(userId, orderId);
            if (order == null) {
                return ApiResult.failure("未找到id为: " + orderId + " 的订单");
            }
        }
        return ApiResult.success(historyService.getHistoryOrderDetail(orderId));
    }

    @Override
    @PostMapping(value = "/orders/create", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createOrder(@RequestBody OrderVO orderVO) throws IOException {
        final Long userId = UserContext.getRequiredUserId();
        log.info("[{}]创建订单：{}", userId, orderVO.toString());
        orderVO.validate();

        final String refId = IdUtil.generateUniqueId();
        var event = new OrderRequestEvent();
        event.refId = refId;
        event.price = orderVO.price;
        event.quantity = orderVO.quantity;
        event.direction = orderVO.direction;
        event.userId = userId;
        event.createAt = System.currentTimeMillis();

        ResponseEntity<String> errorResponse = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(asyncTimeout, errorResponse);
        deferred.onTimeout(() -> {
            log.warn("订单创建超时：{}", orderVO);
            deferredResultMap.remove(event.refId);
        });
        deferredResultMap.put(event.refId, deferred);
        sendEventService.sendMessage(event);
        return null;
    }

    @Override
    @PostMapping(value = "/order/{orderId}/cancel", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> cancelOrder(@PathVariable Long orderId) throws IOException {
        log.info("取消订单[{}]", orderId);
        final Long userId = UserContext.getRequiredUserId();
        String orderString = tradeEnginApiService.get("/internal/" + userId + "/orders/" + orderId);
        ApiResult result = objectMapper.readValue(orderString, ApiResult.class);
        if (!result.isSuccess()) {
            throw new ApiException(ApiError.ORDER_NOT_FOUND, "该订单不存在");
        }

        final String refId = IdUtil.generateUniqueId();
        var event = new OrderCancelEvent();
        event.refId = refId;
        event.orderId = orderId;
        event.userId = UserContext.getRequiredUserId();
        event.createAt = System.currentTimeMillis();

        ResponseEntity<String> errorResponse = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(asyncTimeout, errorResponse);
        deferred.onTimeout(() -> {
            log.warn("订单[orderId: {}, refId: {}]取消超时", orderId, refId);
            deferredResultMap.remove(event.refId);
        });
        deferredResultMap.put(event.refId, deferred);
        sendEventService.sendMessage(event);
        return deferred;
    }

    @Override
    @GetMapping(value = "/ticks", produces = "application/json")
    public ApiResult getRecentTicks() {
        List<String> tickStrList = redisService.lRange(RedisCache.Key.RECENT_TICKS, 0, -1);
        if (tickStrList == null || tickStrList.isEmpty()) {
            return ApiResult.success("[]");
        }
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (String result : tickStrList) {
            sj.add(result);
        }
        return ApiResult.success(sj.toString());
    }

    @Override
    @GetMapping(value = "/bars/day", produces = "application/json")
    public ApiResult getDayBar() {
        long end = System.currentTimeMillis();
        long start = end - 366 * 86400_000;
        return ApiResult.success(getBars(RedisCache.Key.DAY_BARS, start, end));
    }

    @Override
    @GetMapping(value = "/bars/hour", produces = "application/json")
    public ApiResult getHourBar() {
        long end = System.currentTimeMillis();
        long start = end - 720 * 3600_000;
        return ApiResult.success(getBars(RedisCache.Key.HOUR_BARS, start, end));
    }

    @Override
    @GetMapping(value = "/bar/minute", produces = "application/json")
    public ApiResult getMinBar() {
        long end = System.currentTimeMillis();
        long start = end - 1440 * 60_000;
        return ApiResult.success(getBars(RedisCache.Key.MIN_BARS, start, end));
    }

    @Override
    @GetMapping(value = "/bar/second", produces = "application/json")
    public ApiResult getSecondBar() {
        long end = System.currentTimeMillis();
        long start = end - 3600 * 1_000;
        return ApiResult.success(getBars(RedisCache.Key.SEC_BARS, start, end));
    }

    private String getTimeoutJson() throws JsonProcessingException {
        if (timeoutJson == null) {
            timeoutJson = objectMapper.writeValueAsString(new ApiException(ApiError.OPERATION_TIMEOUT, null, ""));
        }
        return timeoutJson;
    }

    private void onApiResultMessage(String msg) {
        log.info("订阅message: {}", msg);
        try {
            ApiResultMessage message = objectMapper.readValue(msg, ApiResultMessage.class);
            if (message.refId != null) {
                DeferredResult<ResponseEntity<String>> deferred = deferredResultMap.remove(message.refId);
                if (deferred != null) {
                    if (message.error != null) {
                        String error = objectMapper.writeValueAsString(message.error);
                        ResponseEntity<String> errorResponse = new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
                        deferred.setResult(errorResponse);
                    } else {
                        String result = objectMapper.writeValueAsString(ApiResult.success(message.result));
                        ResponseEntity<String> successResponse = new ResponseEntity<>(result, HttpStatus.OK);
                        deferred.setResult(successResponse);
                    }
                }
            }
        } catch (Exception e) {
            log.error("invalid apiResult: {}, {}", msg, e);
        }
    }

    private String getBars(String key, long start, long end) {
        List<String> strResult = redisService.zRangeByScore(key, start, end);
        if (strResult == null || strResult.isEmpty()) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (String result : strResult) {
            sj.add(result);
        }
        return sj.toString();
    }
}
