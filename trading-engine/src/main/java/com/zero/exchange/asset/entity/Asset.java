package com.zero.exchange.asset.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产
 * */
@Data
public class Asset {
    private BigDecimal available;

    private BigDecimal frozen;

    public Asset() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    @JsonIgnore
    public BigDecimal getTotalAsset() {
        return available.add(frozen);
    }

    @Override
    public String toString() {
        return "[available: " + available + ", frozen: " + frozen + "]";
    }
}
