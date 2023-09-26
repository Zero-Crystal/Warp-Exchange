package com.zero.exchange.order;

import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.model.trade.OrderEntity;
import com.zero.exchange.asset.entity.TransferType;
import com.zero.exchange.asset.service.AssetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;


class OrderServiceImplTest {

    private AssetServiceImpl assetServiceImpl;

    private OrderServiceImpl orderService;

    private final Long BASE_ACCOUNT = 0000L;

    private final Long ACCOUNT_A = 1000L;

    @BeforeEach
    public void initTest() {
        assetServiceImpl = new AssetServiceImpl();
        orderService = new OrderServiceImpl(assetServiceImpl);

        //init account A USD
        assetServiceImpl.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
                AssetType.USD, BigDecimal.valueOf(10000), false);
        //init account A BTC
        assetServiceImpl.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
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