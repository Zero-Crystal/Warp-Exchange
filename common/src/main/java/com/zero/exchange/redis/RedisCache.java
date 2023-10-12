package com.zero.exchange.redis;

public interface RedisCache {

    public interface Topic {
        String TRADE_API_RESULT = "trade_api_result";

        String NOTIFICATION = "notification";
    }

    public interface Key {
        String ORDER_BOOK = "_orderbook_";

        String RECENT_TICKS = "_ticks_";

        String DAY_BARS = "_day_bars_";

        String HOUR_BARS = "_hour_bars_";

        String MIN_BARS = "_min_bars_";

        String SEC_BARS = "_sec_bars_";
    }
}
