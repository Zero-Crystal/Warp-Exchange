package com.zero.exchange.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zero.exchange.entity.quotation.*;
import com.zero.exchange.entity.support.AbstractBarEntity;
import com.zero.exchange.enums.BarType;
import com.zero.exchange.message.AbstractMessage;
import com.zero.exchange.message.TickMessage;
import com.zero.exchange.messaging.MessageConsumer;
import com.zero.exchange.messaging.Messaging;
import com.zero.exchange.messaging.MessagingFactory;
import com.zero.exchange.redis.RedisCache;
import com.zero.exchange.redis.RedisService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.util.IpUtil;
import com.zero.exchange.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

@Service
public class QuotationService extends LoggerSupport {

    @Autowired
    private RedisService redisService;

    @Autowired
    private MessagingFactory messagingFactory;

    @Autowired
    private QuotationDbService quotationDbService;

    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();

    private MessageConsumer consumer;

    private String shaUpdateRecentTicks = null;

    private String shaUpdateBar = null;

    private long lastSequenceId = 0;

    @PostConstruct
    public void init() {
        shaUpdateRecentTicks = redisService.loadScriptFromClassPath("/redis/update-recent-ticks.lua");
        shaUpdateBar = redisService.loadScriptFromClassPath("/redis/update-bar.lua");
        // init mq:
        String groupId = Messaging.Topic.TICK.name() + "_" + IpUtil.getHostId();
        consumer = messagingFactory.createBatchMessageListener(Messaging.Topic.TICK, groupId, this::processMessage);
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.stop();
            consumer = null;
        }
    }

    void processMessage(List<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            processMessage((TickMessage) message);
        }
    }

    private void processMessage(TickMessage message) {
        // 忽略重复消息
        if (message.sequenceId < lastSequenceId) {
            return;
        }
        log.info(message.toString());

        lastSequenceId = message.sequenceId;

        StringJoiner tickJsonSJ = new StringJoiner(", ", "[", "]");
        StringJoiner tickStrSJ = new StringJoiner(", ", "[", "]");
        long createAt = message.createAt;
        BigDecimal open = BigDecimal.ZERO;
        BigDecimal high = BigDecimal.ZERO;
        BigDecimal low = BigDecimal.ZERO;
        BigDecimal close = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        for (TickEntity tick : message.ticks) {
            String json = tick.toJson();
            tickJsonSJ.add(json);
            tickStrSJ.add("\"" + json + "\"");
            if (close.signum() == 0) {
                open = tick.price;
                high = tick.price;
                low = tick.price;
                close = tick.price;
            } else {
                high = high.max(tick.price);
                low = low.min(tick.price);
                close = tick.price;
            }
            quantity = quantity.add(tick.quantity);
        }

        String tickData = tickJsonSJ.toString();
        if (log.isDebugEnabled()) {
            log.debug("generated ticks data: {}", tickData);
        }
        boolean isRecentTicksUpdate = redisService.executeScriptReturnBoolean(shaUpdateRecentTicks,
                new String[] { RedisCache.Key.RECENT_TICKS },
                new String[] { String.valueOf(lastSequenceId), tickData, tickStrSJ.toString() });
        if (!isRecentTicksUpdate) {
            log.warn("[seqId: {}] ticks ignored by redis", lastSequenceId);
        }

        // 保存数据库
        quotationDbService.saveTicks(message.ticks);

        long sec = createAt / 1000;
        long min = sec / 60;
        long hour = min / 60;
        // 秒K的开始时间
        long secStartTime = sec * 1000;
        // 分钟K的开始时间
        long minStartTime = min * 60 * 1000;
        // 小时K的开始时间
        long hourStartTime = hour * 60 * 60 * 1000;
        // 日K的开始时间，与TimeZone相关
        long dayStartTime = Instant.ofEpochMilli(hourStartTime).atZone(zoneId).withHour(0).toEpochSecond() * 10000;

        String strCreateBars = redisService.executeScriptReturnString(shaUpdateBar,
                new String[] { RedisCache.Key.SEC_BARS, RedisCache.Key.MIN_BARS, RedisCache.Key.HOUR_BARS,
                        RedisCache.Key.DAY_BARS },
                new String[] {
                        String.valueOf(lastSequenceId),
                        String.valueOf(secStartTime),
                        String.valueOf(minStartTime),
                        String.valueOf(hourStartTime),
                        String.valueOf(dayStartTime),
                        String.valueOf(open),
                        String.valueOf(high),
                        String.valueOf(low),
                        String.valueOf(close),
                        String.valueOf(quantity)
                });
        // 将K线保存至数据库
        Map<BarType, BigDecimal[]> bars = JsonUtil.readJson(strCreateBars, TYPE_BARS);
        if (!bars.isEmpty()) {
            SecBarEntity secBar = createBars(SecBarEntity::new, bars.get(BarType.SEC));
            MinBarEntity minBar = createBars(MinBarEntity::new, bars.get(BarType.MIN));
            HourBarEntity hourBar = createBars(HourBarEntity::new, bars.get(BarType.HOUR));
            DayBarEntity dayBar = createBars(DayBarEntity::new, bars.get(BarType.DAY));
            quotationDbService.saveBars(secBar, minBar, hourBar, dayBar);
        }
    }

    private <T extends AbstractBarEntity> T createBars(Supplier<T> fn, BigDecimal[] data) {
        if (data == null) {
            return null;
        }
        T t = fn.get();
        t.startTime = data[0].longValue();
        t.openPrice = data[1];
        t.highPrice = data[2];
        t.lowPrice = data[3];
        t.closePrice = data[4];
        t.quantity = data[5];
        return t;
    }

    private static final TypeReference<Map<BarType, BigDecimal[]>> TYPE_BARS = new TypeReference<>() {
    };

}
