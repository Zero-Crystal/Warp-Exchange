package com.zero.exchange.service;

import com.zero.exchange.redis.RedisCache;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.websocket.PushVertical;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.types.BulkType;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PushService extends LoggerSupport {

    @Value("${server.port}")
    private int serverPort;

    @Value(("${exchange.config.hmac-key}"))
    private String hmacKey;

    @Value("${spring.redis.standalone.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.standalone.port:6379}")
    private int redisPort;

    @Value("${spring.redis.standalone.password:}")
    private String redisPassword;

    @Value("${spring.redis.standalone.database:0}")
    private int redisDatabase = 0;

    private Vertx vertx;

    @PostConstruct
    public void startVertx() {
        vertx = Vertx.vertx();

        var push = new PushVertical(hmacKey, serverPort);
        vertx.deployVerticle(push);

        String redisUrl = "redis://" + (redisPassword.isEmpty() ? "" : ":" + redisPassword + "@") + redisHost
                + ":" + redisPort + "/" + redisDatabase;
        Redis redis = Redis.createClient(vertx, redisUrl);
        log.info("create redis client: {}", redisUrl);

        redis.connect().onSuccess(redisConnection -> {
            log.info("redis connect success.");
            redisConnection.handler(response -> {
                if (response.size() == 3) {
                    Response resp = response.get(2);
                    if (resp instanceof BulkType) {
                        String text = resp.toString();
                        log.debug("receive push message: {}", text);
                        push.broadcast(text);
                    }
                }
            });
            log.info("redis try to subscribe...");
            redisConnection.send(Request.cmd(Command.SUBSCRIBE).arg(RedisCache.Topic.NOTIFICATION)).onSuccess(response -> {
                log.info("redis subscribe NOTIFICATION success.");
            }).onFailure(error -> {
                log.error("redis subscribe failed: {}", error.getMessage(), error);
                exit(1);
            });
        }).onFailure(error -> {
            log.error("redis connect failed.");
            exit(1);
        });
    }

    void exit(int exitCode) {
        this.vertx.close();
        System.exit(exitCode);
    }
}
