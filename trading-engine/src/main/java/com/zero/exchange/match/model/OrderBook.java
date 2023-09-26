package com.zero.exchange.match.model;

import com.zero.exchange.bean.OrderBookItemBean;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.model.trade.OrderEntity;

import java.util.*;

public class OrderBook {
    public final Direction direction;

    public final TreeMap<OrderKey, OrderEntity> book;

    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }

    public OrderEntity getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    public boolean remove(OrderEntity order) {
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }

    public boolean add(OrderEntity order) {
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }

    public boolean exist(OrderEntity order) {
        return this.book.containsKey(new OrderKey(order.sequenceId, order.price));
    }

    public int size() {
        return this.book.size();
    }

    public List<OrderBookItemBean> getOrderBooks(int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>();
        OrderBookItemBean preOrderBook = null;
        for (OrderKey orderKey : this.book.keySet()) {
            OrderEntity order = this.book.get(orderKey);
            if (preOrderBook == null) {
                preOrderBook = new OrderBookItemBean(order.price, order.quantity);
                items.add(preOrderBook);
            } else {
                if (order.price.compareTo(preOrderBook.price) == 0) {
                    preOrderBook.addQuality(order.quantity);
                } else {
                    if (items.size() == maxDepth) {
                        break;
                    }
                    preOrderBook = new OrderBookItemBean(order.price, order.quantity);
                    items.add(preOrderBook);
                }
            }
        }
        return items;
    }

    @Override
    public String toString() {
        if (this.book.isEmpty()) {
            return "order book is empty";
        }
        List<String> orders = new ArrayList<>();
        for (Map.Entry<OrderKey, OrderEntity> bookEntry : this.book.entrySet()) {
            OrderEntity order = bookEntry.getValue();
            orders.add("    " + order.price + " - " + order.unfilledQuantity + ": " + order);
        }
        return String.join("\n", orders);
    }

    public static final Comparator<OrderKey> SORT_SELL = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格从低到高
            int compare = o1.price().compareTo(o2.price());
            return compare == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : compare;
        }
    };

    public static final Comparator<OrderKey> SORT_BUY = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格从高到底
            int compare = o2.price().compareTo(o1.price());
            return compare == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : compare;
        }
    };
}
