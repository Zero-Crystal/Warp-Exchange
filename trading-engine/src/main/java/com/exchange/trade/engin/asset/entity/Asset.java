package com.exchange.trade.engin.asset.entity;

import java.math.BigDecimal;

/**
 * 资产
 * */
public class Asset {
    public BigDecimal available;

    public BigDecimal frozen;

    public Asset() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    @Override
    public String toString() {
        return "Asset: {" +
                "available=" + available +
                ", frozen=" + frozen +
                '}';
    }
}
