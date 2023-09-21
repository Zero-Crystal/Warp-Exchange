package com.exchange.trade.engin.clearing;

import com.exchange.common.enums.AssetType;
import com.exchange.common.model.trade.OrderEntity;
import com.exchange.trade.engin.asset.entity.TransferType;
import com.exchange.trade.engin.asset.service.AssetService;
import com.exchange.trade.engin.match.model.MatchDetailRecord;
import com.exchange.trade.engin.match.model.MatchResult;
import com.exchange.trade.engin.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
                    BigDecimal matched = matchDetail.quality();
                    if (takerOrder.price.compareTo(maker.price) > 0) {
                        // 当买入价格比卖出价格低时，退还差价
                        BigDecimal unfreezeQuote = takerOrder.price.subtract(maker.price).multiply(matchDetail.quality());
                        assetService.assetUnFreeze(takerOrder.sequenceId, AssetType.USD, unfreezeQuote);
                    }
                    // 将买方USD转入卖方账户
                    assetService.transfer(TransferType.FROZEN_TO_AVAILABLE,
                            takerOrder.accountId, maker.accountId, AssetType.USD,
                            maker.price.multiply(matched), false);
                    // 将卖方BTC转入买方账户
                    assetService.transfer(TransferType.FROZEN_TO_AVAILABLE,
                            maker.accountId, takerOrder.accountId, AssetType.BTC, matched, false);
                    // 删除完全成交的maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.accountId, maker.id);
                    }
                }
                // 删除完全成交的taker
                if (takerOrder.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(takerOrder.accountId, takerOrder.id);
                }
            }
            case SELL -> {
                for (MatchDetailRecord matchDetail : matchResult.matchDetails) {
                    OrderEntity maker = matchDetail.makerOrder();
                    BigDecimal matched = matchDetail.quality();
                    // 将卖方的BTC转入买方的账户
                    assetService.transfer(TransferType.FROZEN_TO_AVAILABLE, takerOrder.accountId, maker.accountId,
                            AssetType.BTC, matched, false);
                    // 将买方的USD转入卖方的账户
                    assetService.transfer(TransferType.FROZEN_TO_AVAILABLE, maker.accountId, takerOrder.accountId,
                            AssetType.USD, maker.price.multiply(matched), false);
                    // 删除完全成交的maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.accountId, maker.id);
                    }
                }
                // 删除完全成交的taker
                if (takerOrder.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(takerOrder.accountId, takerOrder.id);
                }
            }
            default -> throw new IllegalArgumentException("位置交易方向");
        }
    }

    @Override
    public void clearingCancel(OrderEntity order) {
        switch (order.direction) {
            case BUY -> {
                assetService.assetUnFreeze(order.accountId, AssetType.USD, order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                assetService.assetUnFreeze(order.accountId, AssetType.BTC, order.unfilledQuantity);
            }
            default -> {
                throw new IllegalArgumentException("未知交易方向");
            }
        }
        orderService.removeOrder(order.accountId, order.id);
    }
}
