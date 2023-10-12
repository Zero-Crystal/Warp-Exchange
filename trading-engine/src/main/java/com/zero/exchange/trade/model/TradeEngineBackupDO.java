package com.zero.exchange.trade.model;

import com.zero.exchange.entity.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TradeEngineBackupDO {

    public long sequenceId;

    public Map<Long, BigDecimal[]> assets;

    public List<OrderEntity> orders;

    public MatchBackup match;

    public static class MatchBackup {
        public List<Long> BUY;
        public List<Long> SELL;
        public BigDecimal marketPrice;
    }
}
