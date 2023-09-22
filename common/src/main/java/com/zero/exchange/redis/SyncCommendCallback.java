package com.zero.exchange.redis;

import io.lettuce.core.api.sync.RedisCommands;

@FunctionalInterface
public interface SyncCommendCallback<T> {

    T doInConnection(RedisCommands<String, String> commands);
}
