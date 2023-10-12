package com.zero.exchange.model;

import com.zero.exchange.util.JsonUtil;

import java.math.BigDecimal;
import java.util.List;

public class OrderBookBean {

    public static final String EMPTY = JsonUtil.writeJson(new OrderBookBean(0, BigDecimal.ZERO, List.of(), List.of()));

    public long sequenceId;

    public BigDecimal price;

    public List<OrderBookItemBean> sell;

    public List<OrderBookItemBean> buy;

    public OrderBookBean(long sequenceId, BigDecimal price, List<OrderBookItemBean> sell, List<OrderBookItemBean> buy) {
        this.sequenceId = sequenceId;
        this.price = price;
        this.sell = sell;
        this.buy = buy;
    }
}
