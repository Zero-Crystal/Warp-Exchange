package com.zero.exchange.model;

import java.math.BigDecimal;

public class OrderBookItemBean {

    public BigDecimal price;

    public BigDecimal quantity;

    public OrderBookItemBean(BigDecimal price, BigDecimal quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public void addQuality(BigDecimal quality) {
        this.quantity = this.quantity.add(quality);
    }
}
