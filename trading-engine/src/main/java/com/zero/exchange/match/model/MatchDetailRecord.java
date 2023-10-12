package com.zero.exchange.match.model;

import com.zero.exchange.entity.trade.OrderEntity;

import java.math.BigDecimal;

public record MatchDetailRecord(
        BigDecimal price,
        BigDecimal quantity,
        OrderEntity takerOrder,
        OrderEntity makerOrder
) {
}
