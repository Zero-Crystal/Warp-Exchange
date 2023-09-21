package com.exchange.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class TradeUtil {

    @Autowired(required = false)
    private ZoneId zoneId = ZoneId.systemDefault();

    /**
     * 创建订单id
     *
     * @param createAt
     * @param sequenceId
     * */
    public Long createOrderId(long createAt, long sequenceId) {
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(createAt).atZone(zoneId);
        Integer year = zonedDateTime.getYear();
        Integer month = zonedDateTime.getMonth().getValue();
        return sequenceId * 10000 + (year * 100 + month);
    }
}
