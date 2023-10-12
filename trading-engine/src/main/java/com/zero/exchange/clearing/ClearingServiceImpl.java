package com.zero.exchange.clearing;

import com.zero.exchange.enums.AssetType;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.asset.service.AssetService;
import com.zero.exchange.asset.model.TransferType;
import com.zero.exchange.match.model.MatchDetailRecord;
import com.zero.exchange.match.model.MatchResult;
import com.zero.exchange.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ClearingServiceImpl implements ClearingService {

    private final AssetService assetService;

    private final OrderService orderService;

    public ClearingServiceImpl(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    @Override
    public void clearingMatchResult(MatchResult matchResult) {
        OrderEntity takerOrder = matchResult.takerOrder;
        switch (matchResult.takerOrder.direction) {
            case BUY -> {
                for (MatchDetailRecord matchDetail : matchResult.matchDetails) {
                    OrderEntity maker = matchDetail.makerOrder();
                    BigDecimal matched = matchDetail.quantity();
                    if (takerOrder.price.compareTo(maker.price) > 0) {
                        // 当买入价格比卖出价格低时，退还差价
                        BigDecimal unfreezeQuote = takerOrder.price.subtract(maker.price).multiply(matchDetail.quantity());
                        assetService.assetUnFreeze(takerOrder.userId, AssetType.USD, unfreezeQuote);
                    }
                    // 将买方USD转入卖方账户
                    assetService.baseTransfer(TransferType.FROZEN_TO_AVAILABLE,
                            takerOrder.userId, maker.userId, AssetType.USD,
                            maker.price.multiply(matched), false);
                    // 将卖方BTC转入买方账户
                    assetService.baseTransfer(TransferType.FROZEN_TO_AVAILABLE,
                            maker.userId, takerOrder.userId, AssetType.BTC, matched, false);
                    // 删除完全成交的maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.userId, maker.id);
                    }
                }
                // 删除完全成交的taker
                if (takerOrder.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(takerOrder.userId, takerOrder.id);
                }
            }
            case SELL -> {
                for (MatchDetailRecord matchDetail : matchResult.matchDetails) {
                    OrderEntity maker = matchDetail.makerOrder();
                    BigDecimal matched = matchDetail.quantity();
                    // 将卖方的BTC转入买方的账户
                    assetService.baseTransfer(TransferType.FROZEN_TO_AVAILABLE, takerOrder.userId, maker.userId,
                            AssetType.BTC, matched, false);
                    // 将买方的USD转入卖方的账户
                    assetService.baseTransfer(TransferType.FROZEN_TO_AVAILABLE, maker.userId, takerOrder.userId,
                            AssetType.USD, maker.price.multiply(matched), false);
                    // 删除完全成交的maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.userId, maker.id);
                    }
                }
                // 删除完全成交的taker
                if (takerOrder.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(takerOrder.userId, takerOrder.id);
                }
            }
            default -> throw new IllegalArgumentException("未知交易方向");
        }
    }

    @Override
    public void clearingCancel(OrderEntity order) {
        switch (order.direction) {
            case BUY -> {
                assetService.assetUnFreeze(order.userId, AssetType.USD, order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                assetService.assetUnFreeze(order.userId, AssetType.BTC, order.unfilledQuantity);
            }
            default -> {
                throw new IllegalArgumentException("未知交易方向");
            }
        }
        orderService.removeOrder(order.userId, order.id);
    }
}
