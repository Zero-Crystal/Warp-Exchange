package com.zero.exchange.redis;

import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.util.ClassPathUtil;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

@Component
public class RedisService extends LoggerSupport {

    private final RedisClient redisClient;

    private final GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;

    public RedisService(@Autowired RedisConfiguration configuration) {
        // init Redis Client
        RedisURI redisURI = RedisURI.Builder.redis(configuration.getHost(), configuration.getPort())
                .withPassword(configuration.getPassword().toCharArray()).withDatabase(configuration.getDatabase()).build();
        this.redisClient = RedisClient.create(redisURI);
        // init redis connection pool
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(5);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        this.redisConnectionPool = ConnectionPoolSupport.createGenericObjectPool(() -> redisClient.connect(), poolConfig);
    }

    @PreDestroy
    public void shutDown() {
        redisConnectionPool.close();
        redisClient.shutdown();
    }

    /**
     * 加载本地 lua脚本，并返回 SHA1值
     *
     * @param classPath lua脚本存放路径
     * @return String SHA1
     * */
    public String loadScriptFromClassPath(String classPath) {
        String sha = executeSync(commands -> {
            try {
                return commands.scriptLoad(ClassPathUtil.readFile(classPath));
            } catch (IOException e) {
                throw new UncheckedIOException("load class path failed！" + classPath, e);
            }
        });
        if (log.isDebugEnabled()) {
            log.debug("load lua script SHA from class path: {}", classPath);
        }
        return sha;
    }

    /**
     * 加载 lua脚本内容，并返回 SHA1值
     *
     * @param scriptContent lua脚本内容
     * @return String SHA1
     * */
    public String loadScript(String scriptContent) {
        return executeSync(commands -> {
            return commands.scriptLoad(scriptContent);
        });
    }

    /**
     * 执行lua脚本并获取boolean返回值
     *
     * @param sha 脚本的SHA1
     * @param keys
     * @param values
     * @return boolean
     * */
    public boolean executeScriptReturnBoolean(String sha, String[] keys, String[] values) {
        return executeSync(commands -> {
            return commands.evalsha(sha, ScriptOutputType.BOOLEAN, keys, values);
        });
    }

    /**
     * 执行lua脚本并获取String返回值
     *
     * @param sha 脚本的SHA1
     * @param keys
     * @param values
     * @return String
     * */
    public String executeScriptReturnString(String sha, String[] keys, String[] values) {
        return executeSync(commands -> {
            return commands.evalsha(sha, ScriptOutputType.VALUE, keys, values);
        });
    }

    /**
     * 订阅指定Redis channel 的消息
     *
     * @param channel redis频道
     * @param listener 消息回调
     * */
    public void subscribe(String channel, Consumer<String> listener) {
        StatefulRedisPubSubConnection<String, String> connection = this.redisClient.connectPubSub();
        connection.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                listener.accept(message);
            }
        });
        connection.sync().subscribe(channel);
    }

    /**
     * 获取Redis缓存
     *
     * @param key redis key
     * @return String
     * */
    public String get(String key) {
        return executeSync(commands -> {
            return commands.get(key);
        });
    }

    /**
     * 发布Redis缓存
     *
     * @param key
     * @param data
     * */
    public void publish(String key, String data) {
        executeSync(commands -> {
            return commands.publish(key, data);
        });
    }

    /**
     * 获取列表类型的键中指定范围内的元素
     *
     * @param key 键
     * @param start 起始索引
     * @param end 终止索引
     * */
    public List<String> lRange(String key, long start, long end) {
        return executeSync(commands -> {
             return commands.lrange(key, start, end);
        });
    }

    /**
     * 获取有序集合中指定分数范围内的成员
     *
     * @param key 键
     * @param min 最小值
     * @param max 最大值
     * */
    public List<String> zRangeByScore(String key, long min, long max) {
        return executeSync(commands -> {
            return commands.zrangebyscore(key, Range.create(min, max));
        });
    }

    public <T> T executeSync(SyncCommendCallback<T> commendCallback) {
        try(StatefulRedisConnection<String, String> connection = redisConnectionPool.borrowObject()) {
            connection.setAutoFlushCommands(true);
            RedisCommands<String, String> commands = connection.sync();
            return commendCallback.doInConnection(commands);
        } catch (Exception e) {
            log.error("executeSync redis failed. {}", e);
            throw new RuntimeException(e);
        }
    }
}
