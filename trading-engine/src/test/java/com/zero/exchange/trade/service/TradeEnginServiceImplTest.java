package com.zero.exchange.trade.service;

import com.zero.exchange.asset.service.AssetServiceImpl;
import com.zero.exchange.clearing.ClearingServiceImpl;
import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.enums.UserType;
import com.zero.exchange.match.service.MatchServiceImpl;
import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.message.event.OrderCancelEvent;
import com.zero.exchange.message.event.OrderRequestEvent;
import com.zero.exchange.message.event.TransferEvent;
import com.zero.exchange.order.OrderServiceImpl;
import com.zero.exchange.redis.RedisService;
import com.zero.exchange.store.StoreServiceImpl;
import com.zero.exchange.trade.service.TradeEnginServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeEnginServiceImplTest {
    static final Long USER_A = 11111L;
    static final Long USER_B = 22222L;
    static final Long USER_C = 33333L;
    static final Long USER_D = 44444L;

    static final Long[] USERS = { USER_A, USER_B, USER_C, USER_D };

    private long currentSequenceId = 0;

    @Test
    public void testTradingEngine() {
        var engine = createTradingEngine();

        engine.processEvent(depositEvent(USER_A, AssetType.USD, bd("58000")));
        engine.processEvent(depositEvent(USER_B, AssetType.USD, bd("126700")));
        engine.processEvent(depositEvent(USER_C, AssetType.BTC, bd("5.5")));
        engine.processEvent(depositEvent(USER_D, AssetType.BTC, bd("8.6")));
        engine.debug();
        engine.validate();

        engine.processEvent(createOrderRequestEvent(USER_A, Direction.BUY, bd("2207.33"), bd("1.2")));
        engine.processEvent(createOrderRequestEvent(USER_C, Direction.SELL, bd("2215.6"), bd("0.8")));
        engine.processEvent(createOrderRequestEvent(USER_C, Direction.SELL, bd("2921.1"), bd("0.3")));
        engine.debug();
        engine.validate();

        engine.processEvent(createOrderRequestEvent(USER_D, Direction.SELL, bd("2206"), bd("0.3")));
        engine.debug();
        engine.validate();

        engine.processEvent(createOrderRequestEvent(USER_B, Direction.BUY, bd("2219.6"), bd("2.4")));
        engine.debug();
        engine.validate();

        engine.processEvent(createOrderCancelEvent(USER_A, 1L));
        engine.debug();
        engine.validate();
    }

    @Test
    public void testRandom() {
        var engine = createTradingEngine();
        var r = new Random(123456789);
        for (Long user : USERS) {
            engine.processEvent(depositEvent(user, AssetType.USD, random(r, 1000_0000, 2000_0000)));
            engine.processEvent(depositEvent(user, AssetType.BTC, random(r, 1000, 2000)));
        }
        engine.debug();
        engine.validate();

        int low = 20000;
        int high = 40000;
        for (int i = 0; i < 100; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(createOrderRequestEvent(user, Direction.BUY, random(r, low, high), random(r, 1, 5)));
            engine.debug();
            engine.validate();

            engine.processEvent(createOrderRequestEvent(user, Direction.SELL, random(r, low, high), random(r, 1, 5)));
            engine.debug();
            engine.validate();
        }

        assertEquals("35216.4", engine.matchService.getMarketPrice().stripTrailingZeros().toPlainString());
    }

    BigDecimal random(Random random, int low, int height) {
        int n = random.nextInt(low, height);
        int m = random.nextInt(100);
        return new BigDecimal(n + "." + m);
    }

    TradeEnginServiceImpl createTradingEngine() {
        var matchEngine = new MatchServiceImpl();
        var assetService = new AssetServiceImpl();
        var orderService = new OrderServiceImpl(assetService);
        var clearingService = new ClearingServiceImpl(assetService, orderService);
        var storeService = new StoreServiceImpl();

        var engine = new TradeEnginServiceImpl();
        engine.assetService = assetService;
        engine.orderService = orderService;
        engine.matchService = matchEngine;
        engine.clearingService = clearingService;
        engine.storeService = storeService;
        return engine;
    }

    OrderRequestEvent createOrderRequestEvent(Long userId, Direction direction, BigDecimal price, BigDecimal quality) {
        var orderRequestEvent = createEvent(OrderRequestEvent.class);
        orderRequestEvent.userId = userId;
        orderRequestEvent.direction = direction;
        orderRequestEvent.price = price;
        orderRequestEvent.quantity = quality;
        return orderRequestEvent;
    }

    OrderCancelEvent createOrderCancelEvent(Long userId, Long orderId) {
        OrderCancelEvent event = createEvent(OrderCancelEvent.class);
        event.userId = userId;
        event.orderId = orderId;
        return event;
    }

    TransferEvent depositEvent(Long userId, AssetType asset, BigDecimal amount) {
        var event = createEvent(TransferEvent.class);
        event.fromUserId = UserType.DEBT.getUserType();
        event.toUserId = userId;
        event.amount = amount;
        event.assetType = asset;
        event.sufficient = false;
        return event;
    }

    <T extends AbstractEvent> T createEvent(Class<T> clazz) {
        T event;
        try {
            event = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        event.previousId = this.currentSequenceId;
        this.currentSequenceId++;
        event.sequenceId = this.currentSequenceId;
        event.createAt = LocalDateTime.parse("2022-02-22T22:22:22").atZone(ZoneId.of("Z")).toEpochSecond() * 1000
                + this.currentSequenceId;
        return event;
    }

    BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}