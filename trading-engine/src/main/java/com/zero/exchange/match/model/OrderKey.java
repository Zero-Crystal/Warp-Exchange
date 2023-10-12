package com.zero.exchange.match.model;

import java.math.BigDecimal;

public record OrderKey(long sequenceId, BigDecimal price) {
}
