package com.exchange.trade.engin.match.model;

import com.exchange.common.module.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MatchResult {
    public final OrderEntity takerOrder;

    public final List<MatchDetailRecord> matchDetails = new ArrayList<>();

    public MatchResult(OrderEntity takerOrder) {
        this.takerOrder = takerOrder;
    }

    public boolean add(BigDecimal price, BigDecimal quality, OrderEntity takerOrder, OrderEntity makerOrder) {
        return matchDetails.add(new MatchDetailRecord(price, quality, takerOrder, makerOrder));
    }

    @Override
    public String toString() {
        if (matchDetails.isEmpty()) {
            return "未查询到任何撮合记录";
        }
        return matchDetails.size() + " matched: "
                + String.join(", ", this.matchDetails.stream().map(MatchDetailRecord::toString).toArray(String[]::new));
    }
}
