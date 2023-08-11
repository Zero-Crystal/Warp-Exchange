package com.exchange.trade.engin.order;

import com.exchange.common.enums.AssetType;
import com.exchange.common.enums.Direction;
import com.exchange.common.module.trade.OrderEntity;
import com.exchange.trade.engin.asset.entity.TransferType;
import com.exchange.trade.engin.asset.service.AssetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;


class OrderServerImplTest {

    private AssetServiceImpl assetServiceImpl;

    private OrderServerImpl orderService;

    private final Long BASE_ACCOUNT = 0000L;

    private final Long ACCOUNT_A = 1000L;

    @BeforeEach
    public void initTest() {
        assetServiceImpl = new AssetServiceImpl();
        orderService = new OrderServerImpl(assetServiceImpl);

        //init account A USD
        assetServiceImpl.transfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
                AssetType.USD, BigDecimal.valueOf(10000), false);
        //init account A BTC
        assetServiceImpl.transfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
                AssetType.BTC, BigDecimal.valueOf(12000), false);
    }

    @Test
    void createOrder() {
        System.out.println("================================================================");
        long sequenceId = System.currentTimeMillis();
        Long orderId = sequenceId + today();
        OrderEntity order = orderService.createOrder(System.currentTimeMillis(), orderId, sequenceId, ACCOUNT_A,
                BigDecimal.valueOf(200), Direction.BUY,  BigDecimal.valueOf(10));
        System.out.println(order.toString());
        System.out.println("================================================================");
    }

    private Long today() {
        return new Date().getTime();
    }
}