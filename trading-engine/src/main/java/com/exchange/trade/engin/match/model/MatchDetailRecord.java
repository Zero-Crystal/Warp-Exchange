package com.exchange.trade.engin.match.model;

import com.exchange.common.model.trade.OrderEntity;

import java.math.BigDecimal;

public record MatchDetailRecord(
        BigDecimal price,
        BigDecimal quality,
        OrderEntity takerOrder,
        OrderEntity makerOrder
) {
}
