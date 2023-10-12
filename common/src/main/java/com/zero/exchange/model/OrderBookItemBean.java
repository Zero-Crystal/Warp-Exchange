package com.zero.exchange.model;

import java.math.BigDecimal;

public class OrderBookItemBean {

    public BigDecimal price;

    public BigDecimal quality;

    public OrderBookItemBean(BigDecimal price, BigDecimal quality) {
        this.price = price;
        this.quality = quality;
    }

    public void addQuality(BigDecimal quality) {
        this.quality = this.quality.add(quality);
    }
}
