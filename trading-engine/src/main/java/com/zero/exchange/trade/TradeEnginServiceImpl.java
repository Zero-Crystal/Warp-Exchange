package com.zero.exchange.trade;

import com.zero.exchange.bean.OrderBookBean;
import com.zero.exchange.enums.AccountType;
import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.enums.MatchType;
import com.zero.exchange.message.ApiResultMessage;
import com.zero.exchange.message.NotificationMessage;
import com.zero.exchange.message.TickMessage;
import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.message.event.OrderCancelEvent;
import com.zero.exchange.message.event.OrderRequestEvent;
import com.zero.exchange.message.event.TransferEvent;
import com.zero.exchange.model.messaging.MessageConsumer;
import com.zero.exchange.model.messaging.MessageProducer;
import com.zero.exchange.model.messaging.MessageTopic;
import com.zero.exchange.model.messaging.MessagingFactory;
import com.zero.exchange.model.quotation.TickEntity;
import com.zero.exchange.model.trade.MatchDetailEntity;
import com.zero.exchange.model.trade.OrderEntity;
import com.zero.exchange.redis.RedisCache;
import com.zero.exchange.redis.RedisService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.asset.entity.Asset;
import com.zero.exchange.asset.entity.TransferType;
import com.zero.exchange.asset.service.AssetService;
import com.zero.exchange.clearing.ClearingService;
import com.zero.exchange.match.service.MatchService;
import com.zero.exchange.store.StoreService;
import com.zero.exchange.match.model.MatchDetailRecord;
import com.zero.exchange.match.model.MatchResult;
import com.zero.exchange.order.OrderService;
import com.zero.exchange.util.IpUtil;
import com.zero.exchange.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Service
public class TradeEnginServiceImpl extends LoggerSupport implements TradeEnginService {

    @Autowired(required = false)
    private ZoneId zoneId = ZoneId.systemDefault();

    @Autowired
    private AssetService assetService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private ClearingService clearingService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private MessagingFactory messagingFactory;

    @Value("#{exchangeConfiguration.orderBookDepth}")
    private Integer orderBookDepth = 100;

    @Value("#{exchangeConfiguration.isDebugMode}")
    private boolean isDebugMode = true;

    private MessageProducer<TickMessage> producer;

    private MessageConsumer consumer;

    /**
     * 收集已完成的订单
     * */
    private Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    /**
     * 收集已完成撮合的撮合结果
     * */
    private Queue<List<MatchDetailEntity>> matchQueue = new ConcurrentLinkedQueue<>();
    /**
     * ...
     * */
    private Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    /**
     * 收集订单消息
     * */
    private Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();
    /**
     * 收集订单创建结果
     * */
    private Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();

    /**
     * 数据库存储线程
     * */
    private Thread dbThread;
    /**
     * ...
     * */
    private Thread tickThread;
    /**
     * 发送订单消息通知
     * */
    private Thread notifyThread;
    /**
     * 发送订单创建结果消息
     * */
    private Thread apiResultThread;
    /**
     * 更新orderBook
     * */
    private Thread orderBooKThread;

    /**
     * 系统是否发生错误
     * */
    private boolean isSystemError = false;
    /**
     * 上一条消息的定序id
     * */
    private long lastSequenceId;
    /**
     * orderBook是否有更新
     * */
    private boolean isOrderBookUpdate = false;
    /**
     * 最新的orderBook
     * */
    private OrderBookBean lastOrderBook = null;
    /**
     * OrderBook更新lua脚本SHA1值
     * */
    private String shaUpdateOrderBookLua = null;

    @PostConstruct
    public void init() {
        shaUpdateOrderBookLua = redisService.loadScriptFromClassPath("/redis/update-orderBook.lua");
        producer = messagingFactory.createMessageProducer(MessageTopic.Topic.TICK, TickMessage.class);
        consumer = messagingFactory.createBatchMessageListener(MessageTopic.Topic.TRADE,
                IpUtil.getHostId(), this::receiveMessage);
        dbThread = new Thread(this::runOnDbThread, "async-db");
        dbThread.start();
        tickThread = new Thread(this::runOnTickThread, "async-tick");
        tickThread.start();
        notifyThread = new Thread(this::runOnNotifyThread, "async-notify");
        notifyThread.start();
        apiResultThread = new Thread(this::runOnApiResultThread, "async-api");
        apiResultThread.start();
        orderBooKThread = new Thread(this::runOnOrderBookThread, "async-orderBook");
        orderBooKThread.start();
    }

    @PreDestroy
    public void destroy() {
        this.consumer.stop();
        this.orderBooKThread.interrupt();
        this.dbThread.interrupt();
    }

    @Override
    public void receiveMessage(List<AbstractEvent> receiveMessages) {
        isOrderBookUpdate = false;
        for (AbstractEvent message : receiveMessages) {
            processEvent(message);
        }
        if (isOrderBookUpdate) {
            lastOrderBook = this.matchService.getOrderBook(orderBookDepth);
        }
    }

    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (isSystemError) {
            return;
        }
        // 1.判断是否是重复消息
        if (abstractEvent.sequenceId <= this.lastSequenceId) {
            log.warn("消息重复, sequenceId={}", abstractEvent.sequenceId);
            return;
        }
        // 2.判断是否丢失了消息
        if (abstractEvent.previousId > this.lastSequenceId) {
            List<AbstractEvent> lostEvents = storeService.loadEventsFromDB(lastSequenceId);
            if (lostEvents.isEmpty()) {
                // 读取失败
                System.exit(1);
                return;
            }
            // 重新分发消息
            for (AbstractEvent event : lostEvents) {
                processEvent(event);
                return;
            }
        }
        // 3.判断当前消息是否指向上一条消息
        if (abstractEvent.previousId != this.lastSequenceId) {
            System.exit(1);
        }
        // 4.处理事件
        if (abstractEvent instanceof OrderRequestEvent) {
            createOrder((OrderRequestEvent) abstractEvent);
        } else if (abstractEvent instanceof OrderCancelEvent) {
            cancelOrder((OrderCancelEvent) abstractEvent);
        } else if (abstractEvent instanceof TransferEvent) {
            transfer((TransferEvent) abstractEvent);
        }
        // 更新 lastSequenceId
        this.lastSequenceId = abstractEvent.sequenceId;
        // debug模式下，验证消息内部状态的完整性
        if (isDebugMode) {
            validate();
        }
    }

    @Override
    public void createOrder(OrderRequestEvent event) {
        // 创建订单
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(event.createAt).atZone(zoneId);
        Integer year = zonedDateTime.getYear();
        Integer month = zonedDateTime.getMonth().getValue();
        Long orderId = event.sequenceId * 10000 + (year * 100 + month);
        OrderEntity order = orderService.createOrder(event.createAt, orderId, event.sequenceId, event.accountId, event.price,
                event.direction, event.quantity);
        if (order == null) {
            log.error("订单[{}]创建失败：{}", orderId, event);
            // 推送失败消息
            apiResultQueue.add(ApiResultMessage.createOrderFailed(event.refId, event.createAt));
        }
        // 撮合
        MatchResult matchResult = matchService.matchOrder(event.sequenceId, order);
        // 清算
        clearingService.clearingMatchResult(matchResult);
        // 推送成功结果，异步推送
        apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, event.createAt, order.copy()));
        // 收集Notification
        List<NotificationMessage> notifyList = new ArrayList<>();
        notifyList.add(createNotificationMessage(event.createAt, "order_matched", order.accountId, order.copy()));
        // 清算结束，收集已完成的订单
        if (!matchResult.matchDetails.isEmpty()) {
            List<OrderEntity> closeOrders = new ArrayList<>();
            List<MatchDetailEntity> matchDetails = new ArrayList<>();
            List<TickEntity> ticks = new ArrayList<>();
            if (matchResult.takerOrder.status.isFinalStatus()) {
                closeOrders.add(matchResult.takerOrder);
            }
            for (MatchDetailRecord detailRecord : matchResult.matchDetails) {
                OrderEntity maker = detailRecord.makerOrder();
                notifyList.add(createNotificationMessage(event.createAt, "order_matched", maker.accountId, maker.copy()));
                if (maker.status.isFinalStatus()) {
                    closeOrders.add(maker);
                }
                // 收集撮合结果
                MatchDetailEntity takerMatch = generateMatchDetailEntity(event.sequenceId, event.createAt, detailRecord, true);
                MatchDetailEntity makerMatch = generateMatchDetailEntity(event.sequenceId, event.createAt, detailRecord, false);
                matchDetails.add(takerMatch);
                matchDetails.add(makerMatch);
                // 收集ticks
                TickEntity tick = new TickEntity();
                tick.sequenceId = event.sequenceId;
                tick.makerOrderId = maker.id;
                tick.takerOrderId = detailRecord.takerOrder().id;
                tick.takerDirection = detailRecord.takerOrder().direction == Direction.BUY;
                tick.price = detailRecord.price();
                tick.quantity = detailRecord.quantity();
                tick.createdAt = event.createAt;
                ticks.add(tick);
            }
            // 异步写入数据库
            orderQueue.add(closeOrders);
            matchQueue.add(matchDetails);
            // 异步发送Tick消息:
            TickMessage tickMessage = new TickMessage();
            tickMessage.ticks = ticks;
            tickMessage.sequenceId = event.sequenceId;
            tickMessage.createAt = event.createAt;
            tickQueue.add(tickMessage);
            // 异步发送消息通知
            notificationQueue.addAll(notifyList);
        }
    }

    @Override
    public void cancelOrder(OrderCancelEvent event) {
        OrderEntity order = orderService.getOrderByOrderId(event.orderId);
        // 未找到该订单或该订单不属于该用户
        if (order == null || order.accountId.longValue() != event.accountId.longValue()) {
            log.error("订单取消失败: {}", event);
            // 发送失败消息
            apiResultQueue.add(ApiResultMessage.cancelOrderFailed(event.refId, event.createAt));
            return;
        }
        matchService.cancel(event.createAt, order);
        clearingService.clearingCancel(order);
        // 发送成功消息
        notificationQueue.add(createNotificationMessage(event.createAt, "order_cancel", order.accountId, order));
        apiResultQueue.add(ApiResultMessage.cancelOrderFailed(event.refId, event.createAt));
    }

    @Override
    public boolean transfer(TransferEvent event) {
        boolean isSuccess = assetService.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, event.fromAccount, event.toAccount,
                event.assetType, event.amount, true);
        return isSuccess;
    }

    /**
     * 数据库数据存储线程
     * */
    private void runOnDbThread() {
        log.info("start batch insert into db...");
        for (;;) {
            try {
                saveToDb();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * tick message 发送线程
     * */
    private void runOnTickThread() {
        log.info("start to send tick message...");
        for (;;) {
            List<TickMessage> preSendTicks = new ArrayList<>();
            for (;;) {
                TickMessage message = tickQueue.poll();
                if (message != null) {
                    if (preSendTicks.size() >= 1000) {
                        break;
                    }
                    preSendTicks.add(message);
                } else {
                    break;
                }
            }
            if (!preSendTicks.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("发送 {} 条 tick message", preSendTicks.size());
                }
                producer.sendMessage(preSendTicks);
            } else {
                try {
                    // 暂停 1 ms
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    log.error("send kafka message [{}] is interrupted...", Thread.currentThread().getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * redis 发布匹配成功的订单消息
     * */
    private void runOnNotifyThread() {
        log.info("start to send notification message...");
        for (;;) {
            NotificationMessage message = this.notificationQueue.poll();
            if (message != null) {
                if (log.isDebugEnabled()) {
                    log.debug("使用redis发布一条订单消息: {}", JsonUtil.writeJson(message));
                }
                redisService.publish(RedisCache.Topic.NOTIFICATION, JsonUtil.writeJson(message));
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    log.error("publish redis message [{}] is interrupt....", Thread.currentThread().getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * redis 发布订单创建消息
     * */
    private void runOnApiResultThread() {
        log.info("start to send order api result message...");
        for (;;) {
            ApiResultMessage message = apiResultQueue.poll();
            if (message != null) {
                if (log.isDebugEnabled()) {
                    log.debug("使用redis发布一条订单创建结果消息: {}", JsonUtil.writeJson(message));
                }
                redisService.publish(RedisCache.Topic.TRADE_API_RESULT, JsonUtil.writeJson(message));
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    log.error("publish redis message [{}] is interrupt....", Thread.currentThread().getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 更新OrderBook
     * */
    private void runOnOrderBookThread() {
        long lastSequenceId = 0;
        for (;;) {
            final OrderBookBean orderBook = this.lastOrderBook;
            // 在orderBook更新后刷新orderBook
            if (orderBook != null && orderBook.sequenceId > lastSequenceId) {
                if (log.isDebugEnabled()) {
                    log.debug("Update OrderBook at sequenceId[{}]", orderBook.sequenceId);
                }
                redisService.executeScriptReturnBoolean(shaUpdateOrderBookLua,
                        new String[]{RedisCache.Key.ORDER_BOOK},
                        new String[]{String.valueOf(orderBook.sequenceId), JsonUtil.writeJson(orderBook)});
                lastSequenceId = orderBook.sequenceId;
            } else {
                try {
                    // 暂停 1 ms
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    log.error("publish redis message [{}] is interrupt....", Thread.currentThread().getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将已完成的订单保存至数据库
     * */
    private void saveToDb() throws InterruptedException {
        // 批量存储已完成撮合的撮合结果
        if (!matchQueue.isEmpty()) {
            List<MatchDetailEntity> preSaveMatch = new ArrayList<>(1000);
            for (;;) {
                List<MatchDetailEntity> pollMatchs = matchQueue.poll();
                if (pollMatchs != null) {
                    if (preSaveMatch.size() >= 1000) {
                        break;
                    }
                    preSaveMatch.addAll(pollMatchs);
                } else {
                    break;
                }
            }
            preSaveMatch.sort(MatchDetailEntity::compareTo);
            if (log.isDebugEnabled()) {
                log.debug("批量存储已完成撮合的撮合结果...");
            }
            storeService.insertIgnoreList(preSaveMatch);
        }
        // 批量保存已完全交易成功的订单
        if (!orderQueue.isEmpty()) {
            List<OrderEntity> preSaveOrder = new ArrayList<>(1000);
            for (;;) {
                List<OrderEntity> pollOrders = orderQueue.poll();
                if (pollOrders != null) {
                    if (preSaveOrder.size() >= 1000) {
                        break;
                    }
                    preSaveOrder.addAll(pollOrders);
                } else {
                    break;
                }
            }
            preSaveOrder.sort(OrderEntity::compareTo);
            if (log.isDebugEnabled()) {
                log.debug("批量存储已完成订单...");
            }
            storeService.insertIgnoreList(preSaveOrder);
        }
    }

    /**
     * 创建撮合结果实体类
     *
     * @param sequenceId 定序id
     * @param timestamp 创建时间
     * @param detail 撮合结果
     * @param forTaker boolean 是否是挂单
     * @return MatchDetailEntity
     * */
    private MatchDetailEntity generateMatchDetailEntity(long sequenceId, long timestamp,
                                                        MatchDetailRecord detail, boolean forTaker) {
        MatchDetailEntity mde = new MatchDetailEntity();
        mde.sequenceId = sequenceId;
        mde.orderId = forTaker ? detail.takerOrder().id : detail.makerOrder().id;
        mde.counterOrderId = forTaker ? detail.makerOrder().id : detail.takerOrder().id;
        mde.account = forTaker ? detail.takerOrder().accountId : detail.makerOrder().accountId;
        mde.counterAccount = forTaker ? detail.makerOrder().accountId : detail.takerOrder().accountId;
        mde.type = forTaker ? MatchType.TAKER : MatchType.MACKER;
        mde.direction = forTaker ? detail.takerOrder().direction : detail.makerOrder().direction;
        mde.price = detail.price();
        mde.quantity = detail.quantity();
        mde.createdAt = timestamp;
        return mde;
    }

    /**
     * 创建消息通知实体类
     *
     * @param ts long 时间戳
     * @param type 消息类型
     * @param account 用户账号
     * @param data 消息数据
     * @return NotificationMessage
     * */
    private NotificationMessage createNotificationMessage(long ts, String type, Long account, Object data) {
        NotificationMessage message = new NotificationMessage();
        message.createAt = ts;
        message.type = type;
        message.account = account;
        message.data = data;
        return message;
    }

    /**
     * 验证消息内部状态
     * */
    private void validate() {
        log.info("--------------------start validate trade system--------------------");
        long startTime = System.currentTimeMillis();
        validateAsset();
        validateOrder();
        validateMatch();
        long costTime = System.currentTimeMillis() - startTime;
        log.info("----------------------validate end cost {} ms----------------------", costTime);
    }

    /**
     * 验证资产系统总额为0，且除负债账户外其余账户资产不为负；
     * */
    private void validateAsset() {
        // 验证系统资产完整性:
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Map.Entry<Long, ConcurrentMap<AssetType, Asset>> entry : assetService.getUserAssets().entrySet()) {
            Long userAccount = entry.getKey();
            ConcurrentMap<AssetType, Asset> assetMap = entry.getValue();
            for (Map.Entry<AssetType, Asset> assetEntry : assetMap.entrySet()) {
                AssetType assetType = assetEntry.getKey();
                Asset asset = assetEntry.getValue();
                if (userAccount == AccountType.DEBT.getAccountType()) {
                    // 系统负债账户available不允许为正:
                    require(asset.getAvailable().signum() <= 0, "负债用户[" + userAccount + "]的可用金额大于0");
                    // 系统负债账户frozen必须为0:
                    require(asset.getFrozen().signum() == 0, "负债用户[" + userAccount + "]的冻结不为0");
                } else if (userAccount == AccountType.TRADER.getAccountType()) {
                    require(asset.getAvailable().signum() >= 0, "交易用户[" + userAccount + "]的可用金额为负数");
                    require(asset.getFrozen().signum() >= 0, "交易用户[" + userAccount + "]的冻结金额为负数");
                }
                switch (assetType) {
                    case USD -> totalUSD = totalUSD.add(asset.getTotalAsset());
                    case BTC -> totalBTC = totalBTC.add(asset.getTotalAsset());
                }
            }
        }
        // 各类别资产总额为0:
        require(totalUSD.signum() == 0, "USD 交易总金额不为0");
        require(totalBTC.signum() == 0, "BTC 交易总金额不为0");
    }

    /**
     * 验证订单系统未成交订单所冻结的资产与资产系统中的冻结一致；
     * */
    private void validateOrder() {
        // 获取订单系统中所有累计为成交的冻结资产
        Map<Long, Map<AssetType, BigDecimal>> userOrderFrozen = new HashMap<>();
        for (Map.Entry<Long, OrderEntity> entry : orderService.getActiveOrders().entrySet()) {
            Long userAccount = entry.getKey();
            OrderEntity order = entry.getValue();
            require(order.unfilledQuantity.signum() > 0, "交易用户[" + userAccount + "]的订单[" + order.id + "]未成交数量<=0");
            switch (order.direction) {
                case BUY -> {
                    // 订单必须在撮合引擎中
                    require(matchService.getOrderBook(Direction.BUY).exist(order), "订单簿中不包含该订单[" + order.id + "]");
                    // 累计未成交冻结的USD
                    userOrderFrozen.putIfAbsent(userAccount, new HashMap<>());
                    Map<AssetType, BigDecimal> userFrozenAsset = userOrderFrozen.get(userAccount);
                    userFrozenAsset.putIfAbsent(AssetType.USD, BigDecimal.ZERO);
                    BigDecimal frozenUSD = userFrozenAsset.get(AssetType.USD);
                    userFrozenAsset.put(AssetType.USD, frozenUSD.add(order.price.multiply(order.unfilledQuantity)));
                    break;
                }
                case SELL -> {
                    // 该售卖订单必须在撮合引擎中
                    require(matchService.getOrderBook(Direction.SELL).exist(order), "售卖订单簿中不包含该订单[" + order.id + "]");
                    // 累计未成交的BTC
                    userOrderFrozen.putIfAbsent(userAccount, new HashMap<>());
                    Map<AssetType, BigDecimal> userFrozenAsset = userOrderFrozen.get(userAccount);
                    userFrozenAsset.putIfAbsent(AssetType.BTC, BigDecimal.ZERO);
                    BigDecimal frozenBTC = userFrozenAsset.get(AssetType.BTC);
                    userFrozenAsset.put(AssetType.BTC, frozenBTC.add(order.price.multiply(order.unfilledQuantity)));
                    break;
                }
                default -> {
                    require(false,
                            "用户[account: " + userAccount + ", direction: " + order.direction + "]的交易方向异常！");
                    break;
                }
            }
        }
        // 验证订单系统中已冻结的未成交资产和资产系统中的一致
        for (Map.Entry<Long, ConcurrentMap<AssetType, Asset>> entry : assetService.getUserAssets().entrySet()) {
            Long userAccount = entry.getKey();
            ConcurrentMap<AssetType, Asset> userAsset = entry.getValue();
            for (Map.Entry<AssetType, Asset> entry1 : userAsset.entrySet()) {
                AssetType assetType = entry1.getKey();
                Asset asset = entry1.getValue();
                Map<AssetType, BigDecimal> orderFrozen = userOrderFrozen.get(userAccount);
                require(orderFrozen !=  null, "用户[" + userAccount + "]未成交的冻结资产在订单系统中不存在");
                BigDecimal frozen = orderFrozen.get(assetType);
                require(frozen != null, "用户[" + userAccount + "]类型为[" + assetType + "]的冻结资产不存在");
                require(frozen.compareTo(asset.getFrozen()) == 0,
                        "用户[account: " + userAccount + ", assetType: " + assetType + "]在订单系统中已冻结的未成交资产和资产系统中的不一致");
                orderFrozen.remove(assetType);
            }
        }
        // 验证userOrderFrozen中不存在未验证的数据
        for (Map.Entry<Long, Map<AssetType, BigDecimal>> entry : userOrderFrozen.entrySet()) {
            Long account = entry.getKey();
            Map<AssetType, BigDecimal> orderFrozenMap = entry.getValue();
            require(orderFrozenMap.isEmpty(), "用户[" + account + "]存在未验证的订单冻结资产");
        }
    }

    /**
     * 验证订单系统的订单与撮合引擎的订单簿一对一存在
     * */
    private void validateMatch() {
        Map<Long, OrderEntity> copyAllActiveOrders = new HashMap<>(orderService.getActiveOrders());
        // 验证订单系统封中所有买入订单都存在于买入订单簿
        for (OrderEntity order : matchService.getOrderBook(Direction.BUY).book.values()) {
            require(copyAllActiveOrders.remove(order.id) == order, "购买订单簿中存在未知订单[" + order.id + "]");
        }
        // 验证订单系统中所有卖出订单都存在于卖出订单簿
        for (OrderEntity order : matchService.getOrderBook(Direction.SELL).book.values()) {
            require(copyAllActiveOrders.remove(order.id) == order, "卖出订单簿中存在未知订单[" + order.id + "]");
        }
        // 验证订单系统中所有活跃的订单都已验证
        require(copyAllActiveOrders.isEmpty(), "订单系统中存在不存在于订单系统的订单");
    }

    /**
     * 校验condition是否满足，不满足则退出
     * */
    private void require(boolean condition, String errorMessage) {
        if (!condition) {
            log.error("validate system error, {}", errorMessage);
            panic();
        }
    }

    /**
     * 系统错误，退出
     * */
    private void panic() {
        log.error("System panic error, exit system....");
        this.isSystemError = true;
        System.exit(1);
    }
}
