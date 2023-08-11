package com.exchange.trade.engin.match.service;

import com.exchange.common.bean.OrderBookBean;
import com.exchange.common.enums.Direction;
import com.exchange.common.enums.OrderStatus;
import com.exchange.common.module.trade.OrderEntity;
import com.exchange.trade.engin.match.model.MatchResult;
import com.exchange.trade.engin.match.model.OrderBook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MatchServiceImpl implements MatchService {

    public final OrderBook BUY_ORDER_BOOK = new OrderBook(Direction.BUY);

    public final OrderBook SELL_ORDER_BOOK = new OrderBook(Direction.SELL);

    public BigDecimal marketPrice = BigDecimal.ZERO;

    private long lastSequenceId = 0;

    @Override
    public MatchResult matchOrder(long sequenceId, OrderEntity order) {
        return switch (order.direction) {
            case BUY -> processOrder(sequenceId, order, this.SELL_ORDER_BOOK, this.BUY_ORDER_BOOK);
            case SELL -> processOrder(sequenceId, order, this.BUY_ORDER_BOOK, this.SELL_ORDER_BOOK);
            default -> throw new IllegalArgumentException("未知交易方向，account=" + order.accountId
                    + ", direction=" + order.direction);
        };
    }

    @Override
    public MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        this.lastSequenceId = sequenceId;
        Long ts = takerOrder.createdAt;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        // 死循环
        for (;;) {
            OrderEntity markerOrder = makerBook.getFirst();
            if (markerOrder == null) {
                break;
            }

            if (takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(markerOrder.price) < 0) {
                // 买入价格比卖盘第一个价格低
                break;
            } else if (takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(markerOrder.price) > 0) {
                // 卖出价格比买盘第一个价格高
                break;
            }

            // 已挂Maker格成交
            this.marketPrice = markerOrder.price;
            // 待成交数量（吃单数量和挂盘数量的最小值）
            BigDecimal matchQuality = takerUnfilledQuantity.min(markerOrder.quantity);
            // 成交记录
            matchResult.add(this.marketPrice, matchQuality,  takerOrder, markerOrder);
            // 更新成交后订单数量
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchQuality);
            BigDecimal makerUnfilledQuality = markerOrder.unfilledQuantity.subtract(matchQuality);
            if (makerUnfilledQuality.signum() == 0) {
                // 完全成交
                markerOrder.updateOrder(makerUnfilledQuality, OrderStatus.FULLY_FILLED, ts);
                // 移除订单记录
                makerBook.remove(markerOrder);
            } else {
                // 部分成交
                markerOrder.updateOrder(makerUnfilledQuality, OrderStatus.PARTIAL_FILLED, ts);
            }

            // 完全成交后退出循环
            if (takerUnfilledQuantity.signum() == 0) {
                break;
            }
        }

        // Taker未完全成交时，放入订单簿
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING : OrderStatus.PARTIAL_FILLED,
                    ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

    @Override
    public void cancel(long ts, OrderEntity order) {
        OrderBook orderBook = order.direction == Direction.BUY ? BUY_ORDER_BOOK : SELL_ORDER_BOOK;
        if (!orderBook.remove(order)) {
            throw new IllegalArgumentException("未找到该交易订单");
        }
        OrderStatus status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatus.FULLY_CANCELLED
                : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrder(order.unfilledQuantity, status, ts);
    }

    @Override
    public OrderBookBean getOrderBook(int maxDepth) {
        return new OrderBookBean(lastSequenceId, marketPrice,
                SELL_ORDER_BOOK.getOrderBooks(maxDepth), BUY_ORDER_BOOK.getOrderBooks(maxDepth));
    }
}
