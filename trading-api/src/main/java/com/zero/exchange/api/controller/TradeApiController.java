package com.zero.exchange.api.controller;

import com.zero.exchange.api.ApiResult;
import com.zero.exchange.api.TradeApi;
import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.message.ApiResultMessage;
import com.zero.exchange.message.event.OrderCancelEvent;
import com.zero.exchange.message.event.OrderRequestEvent;
import com.zero.exchange.model.OrderBookBean;
import com.zero.exchange.context.UserContext;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.model.OrderVO;
import com.zero.exchange.model.UserSignUpVO;
import com.zero.exchange.redis.RedisCache;
import com.zero.exchange.redis.RedisService;
import com.zero.exchange.service.HistoryService;
import com.zero.exchange.service.SendEventService;
import com.zero.exchange.service.TradeEnginApiService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.user.UserService;
import com.zero.exchange.util.IdUtil;
import com.zero.exchange.util.JsonUtil;
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
    private UserService userService;

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
    @PostMapping(value = "/signup", produces = "application/json")
    public ApiResult signUp(@RequestBody UserSignUpVO userSignUpVO) {
        log.info("注册账号：{}", userSignUpVO.toString());
        ApiError apiError = userSignUpVO.validate();
        if (apiError.getCode() != ApiError.OK.getCode()) {
            return ApiResult.failure(apiError);
        }
        return ApiResult.success(userService.signUp(userSignUpVO.email, userSignUpVO.name, userSignUpVO.password));
    }

    @Override
    @GetMapping(value = "/assets", produces = "application/json")
    public ApiResult getAssets() throws IOException {
        String url = "/internal/" + UserContext.getRequiredUserId() + "/assets";
        return tradeEnginApiService.get(url);
    }

    @Override
    @GetMapping(value = "/orders/{orderId}", produces = "application/json")
    public ApiResult getOpenOrder(@PathVariable Long orderId) throws IOException {
        String url = "/internal/" + UserContext.getRequiredUserId() + "/orders";
        return tradeEnginApiService.get(url);
    }

    @Override
    @GetMapping(value = "/orders", produces = "application/json")
    public ApiResult getOpenOrders() throws IOException {
        String url = "/internal/" + UserContext.getRequiredUserId() + "/orders";
        return tradeEnginApiService.get(url);
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
        ApiResult result = tradeEnginApiService.get("/internal/" + userId + "/orders/" + orderId);
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
        ResponseEntity<String> timeout = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(asyncTimeout, timeout);

        ApiError apiError = orderVO.validate();
        if (apiError.getCode() != ApiError.OK.getCode()) {
            ResponseEntity<String> paramInvalid = new ResponseEntity<>(JsonUtil.writeJson(ApiResult.failure(apiError)), HttpStatus.BAD_REQUEST);
            deferred.setResult(paramInvalid);
            return deferred;
        }

        final String refId = IdUtil.generateUniqueId();
        var event = new OrderRequestEvent();
        event.refId = refId;
        event.price = orderVO.price;
        event.quantity = orderVO.quantity;
        event.direction = orderVO.direction;
        event.userId = userId;
        event.createAt = System.currentTimeMillis();
        deferred.onTimeout(() -> {
            log.warn("订单创建超时：{}", orderVO);
            deferredResultMap.remove(event.refId);
        });
        deferredResultMap.put(event.refId, deferred);
        sendEventService.sendMessage(event);
        return deferred;
    }

    @Override
    @PostMapping(value = "/order/{orderId}/cancel", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> cancelOrder(@PathVariable Long orderId) throws IOException {
        log.info("取消订单[{}]", orderId);
        ResponseEntity<String> errorResponse = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(asyncTimeout, errorResponse);

        final Long userId = UserContext.getRequiredUserId();
        ApiResult result = tradeEnginApiService.get("/internal/" + userId + "/orders/" + orderId);
        if (!result.isSuccess()) {
            ResponseEntity<String> paramInvalid = new ResponseEntity<>(JsonUtil.writeJson(result), HttpStatus.BAD_REQUEST);
            deferred.setResult(paramInvalid);
            return deferred;
        }

        final String refId = IdUtil.generateUniqueId();
        var event = new OrderCancelEvent();
        event.refId = refId;
        event.orderId = orderId;
        event.userId = UserContext.getRequiredUserId();
        event.createAt = System.currentTimeMillis();
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

    private String getTimeoutJson() {
        if (timeoutJson == null) {
            timeoutJson = JsonUtil.writeJson(new ApiException(ApiError.OPERATION_TIMEOUT));
        }
        return timeoutJson;
    }

    private void onApiResultMessage(String msg) {
        log.info("订阅message: {}", msg);
        ApiResultMessage message = JsonUtil.readJson(msg, ApiResultMessage.class);
        if (message.refId != null) {
            DeferredResult<ResponseEntity<String>> deferred = deferredResultMap.remove(message.refId);
            if (deferred != null) {
                if (message.error != null) {
                    String error = JsonUtil.writeJson(message.error);
                    ResponseEntity<String> errorResponse = new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
                    deferred.setResult(errorResponse);
                } else {
                    String result = JsonUtil.writeJson(ApiResult.success(message.result));
                    ResponseEntity<String> successResponse = new ResponseEntity<>(result, HttpStatus.OK);
                    deferred.setResult(successResponse);
                }
            }
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
