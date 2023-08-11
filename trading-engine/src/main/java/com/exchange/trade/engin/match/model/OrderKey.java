package com.exchange.trade.engin.match.model;

import java.math.BigDecimal;

public record OrderKey(long sequenceId, BigDecimal price) {
}
