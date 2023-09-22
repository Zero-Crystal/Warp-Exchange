package com.zero.exchange.match.service;

import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.model.trade.OrderEntity;
import com.zero.exchange.asset.entity.TransferType;
import com.zero.exchange.asset.service.AssetServiceImpl;
import com.zero.exchange.match.model.MatchResult;
import com.zero.exchange.match.model.OrderBook;
import com.zero.exchange.order.OrderServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

class MatchServiceTest {
    private MatchServiceImpl matchService;

    private AssetServiceImpl assetServiceImpl;

    private OrderServerImpl orderService;

    private OrderBook SELL_BOOK;

    private OrderBook BUY_BOOK;

    private final Long BASE_ACCOUNT = 0000L;

    private final Long ACCOUNT_A = 1000L;

    private final Long ACCOUNT_B = 2000l;

    @BeforeEach
    void setUp() {
        matchService = new MatchServiceImpl();
        assetServiceImpl = new AssetServiceImpl();
        orderService = new OrderServerImpl(assetServiceImpl);

        //init account A USD
        assetServiceImpl.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
                AssetType.USD, BigDecimal.valueOf(500000), false);
        //init account B BTC
        assetServiceImpl.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_B,
                AssetType.BTC, BigDecimal.valueOf(300000), false);

        //init SELL order book
        SELL_BOOK = new OrderBook(Direction.SELL);
        SELL_BOOK.add(createOrderEntity(ACCOUNT_A, BigDecimal.valueOf(200), BigDecimal.valueOf(10), Direction.SELL));
        SELL_BOOK.add(createOrderEntity(ACCOUNT_A, BigDecimal.valueOf(300), BigDecimal.valueOf(20), Direction.SELL));

        //init BUY order book
        BUY_BOOK = new OrderBook(Direction.BUY);
    }

    @Test
    void processOrder() {
        //AccountB 使用 BTC 买入 AccountA 的 USD
        System.out.println("================================================================");
        OrderEntity order = createOrderEntity(ACCOUNT_B, BigDecimal.valueOf(201), BigDecimal.valueOf(8), Direction.BUY);
        System.out.println(order.toString());
        MatchResult matchResult = matchService.matchOrder(order.sequenceId, order);
        System.out.println(matchResult);
        System.out.println("================================================================");
    }

    private OrderEntity createOrderEntity(Long accountId, BigDecimal price, BigDecimal quality, Direction direction) {
        long sequenceId = System.currentTimeMillis();
        Long orderId = sequenceId + today();
        return orderService.createOrder(System.currentTimeMillis(), orderId, sequenceId, accountId,
                price, direction,  quality);
    }

    private Long today() {
        return new Date().getTime();
    }
}