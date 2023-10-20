package com.zero.exchange.websocket;

import com.zero.exchange.message.NotificationMessage;
import com.zero.exchange.model.AuthToken;
import com.zero.exchange.util.JsonUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 创建基于Vert.x的HTTP服务器（内部使用Netty）；
 * 创建路由；
 * 绑定一个路径为/notification的GET请求，将其升级为WebSocket连接；
 * 绑定其他路径的GET请求；
 * 开始监听指定端口号。
 * */
public class PushVertical extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(PushVertical.class);

    private final String hmacKey;

    private final int serverPort;

    /**
     * handler map
     * */
    private Map<String, Boolean> handlerMap = new HashMap<>(1000);

    /**
     * handle -> userId
     * */
    private Map<String, Long> handlerToUserMap = new HashMap<>(1000);

    /**
     * userId -> handler set
     * */
    private Map<Long, Set<String>> userToHandlerSet = new HashMap<>(1000);

    public PushVertical(String hmacKey, int serverPort) {
        this.hmacKey = hmacKey;
        this.serverPort = serverPort;
    }

    @Override
    public void start() throws Exception {
        // 创建VertX HttpServer:
        HttpServer httpServer = vertx.createHttpServer();
        // 创建路由:
        Router router = Router.router(vertx);
        // 处理请求 GET /notification:
        router.get("/notification").handler(rc -> {
            HttpServerRequest request = rc.request();
            Supplier<Long> supplier = () -> {
                String strToken = request.getParam("token");
                if (Strings.isNotEmpty(strToken)) {
                    AuthToken token = AuthToken.fromSecureString(strToken, hmacKey);
                    if (!token.isExpired()) {
                        return token.userId();
                    }
                }
                return null;
            };
            final Long userId = supplier.get();
            log.info("parse userId from token: {}", userId);
            // 将请求升级为websocket
            request.toWebSocket(arHandler -> {
                if (arHandler.succeeded()) {
                    initWebsocket(arHandler.result(), userId);
                }
            });
        });
        // 处理请求 GET /actuator/health
        router.get("/actuator/health").respond(rc ->
                rc.response().putHeader("Content-type", "application/json").end("{ \"status\": \"UP\" }"));
        // 处理其他请求
        router.get().respond(rc -> rc.response().setStatusCode(404).setStatusMessage("not found").end());

        httpServer.requestHandler(router).listen(serverPort, result -> {
            if (result.succeeded()) {
                log.info("Vertx started on port(s): {} (http) with context path ''", this.serverPort);
            } else {
                log.error("Start http server failed on port " + this.serverPort, result.cause());
                vertx.close();
                System.exit(1);
            }
        });
    }

    public void broadcast(String text) {
        NotificationMessage message = null;
        try {
            message = JsonUtil.readJson(text, NotificationMessage.class);
        } catch (Exception e) {
            log.error("notification json message 转 NotificationMessage 失败：{}", text);
            return;
        }
        if (message.userId == null) {
            log.info("未获取到 notification message 中的 userId，向所有websocket链接发送广播");
            // 向所有websocket链接发送广播
            EventBus eb = vertx.eventBus();
            for (String handlerId : handlerMap.keySet()) {
                eb.send(handlerId, text);
            }
        } else {
            log.info("向 user[{}] 发送广播", message.userId);
            EventBus eb = vertx.eventBus();
            Set<String> handlers = userToHandlerSet.get(message.userId);
            if (handlers != null) {
                for (String handlerId : userToHandlerSet.get(message.userId)) {
                    eb.send(handlerId, text);
                }
            }
        }
    }

    /**
     * 初始化一个 websocket
     * */
    private void initWebsocket(ServerWebSocket webSocket, Long userId) {
        String handlerId = webSocket.textHandlerID();
        webSocket.textMessageHandler(strMessage -> {
            log.info("receive textMessage: {}", strMessage);
        });
        webSocket.exceptionHandler(exceptionHandler -> {
            log.error("websocket happened an exception: {}", exceptionHandler.getMessage(), exceptionHandler);
        });
        webSocket.closeHandler(ch -> {
            unsubscribeClient(handlerId);
            unsubscribeUserHandler(handlerId, userId);
        });
        subscribeClient(handlerId);
        subscribeUserHandler(handlerId, userId);
        if (userId == null) {
            webSocket.writeTextMessage("{\"type\":\"status\",\"status\":\"connected\",\"message\":\"connected as anonymous user\"}");
        } else {
            webSocket.writeTextMessage("{\"type\":\"status\",\"status\":\"connected\",\"message\":\"connected as user: " + userId + "\"}");
        }
    }

    private void subscribeClient(String handlerId) {
        handlerMap.put(handlerId, Boolean.TRUE);
    }

    private void unsubscribeClient(String handlerId) {
        handlerMap.remove(handlerId);
    }

    private void subscribeUserHandler(String handlerId, Long userId) {
        if (userId == null) {
            return;
        }
        handlerToUserMap.put(handlerId, userId);
        Set<String> handlerSet = userToHandlerSet.get(userId);
        if (handlerSet == null) {
            handlerSet = new HashSet<>();
            userToHandlerSet.put(userId, handlerSet);
        }
        handlerSet.add(handlerId);
        log.info("subscribe user websocket is ok: [userId: {}, handlerId: {}]", userId, handlerId);
    }

    private void unsubscribeUserHandler(String handlerId, Long userId) {
        if (userId == null) {
            return;
        }
        handlerToUserMap.remove(handlerId);
        Set<String> handlerSet = userToHandlerSet.get(userId);
        if (handlerSet != null) {
            handlerSet.remove(handlerId);
            if (handlerSet.isEmpty()) {
                userToHandlerSet.remove(userId);
                log.info("unsubscribe user[{}] handler set", userId);
            } else {
                log.info("unsubscribe user[{}] handler, current handler set size is {}", userId, handlerSet.size());
            }
        }
    }
}
