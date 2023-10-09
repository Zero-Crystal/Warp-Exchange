package com.zero.exchange.model;

import com.zero.exchange.enums.MatchType;

import java.math.BigDecimal;

public record OrderMatchedDetailVO(BigDecimal price, BigDecimal quantity, MatchType type) {
}
