package com.zero.exchange.trade.task;

import com.zero.exchange.asset.model.Asset;
import com.zero.exchange.asset.service.AssetService;
import com.zero.exchange.entity.trade.OrderEntity;
import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.Direction;
import com.zero.exchange.match.model.OrderBook;
import com.zero.exchange.match.service.MatchService;
import com.zero.exchange.order.OrderService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.trade.model.TradeEngineBackupDO;
import com.zero.exchange.trade.service.TradeEnginService;
import com.zero.exchange.util.FileUtil;
import com.zero.exchange.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

@ConditionalOnProperty(value = "exchange.config.backup-enable", havingValue = "true")
@Component
public class TradeEngineBackupTask extends LoggerSupport {

    @Autowired
    private AssetService assetService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TradeEnginService tradeEnginService;

    @Value("${exchange.config.backup-path}")
    private String backupPath;

    /**
     * 交易引擎状态备份，每隔 5分钟备份一次：
     * {
     *     "sequenceId": 189000,
     *     "assets": { ... },
     *     "orders": [ ... ],
     *     "match": { ... }
     * }
     * */
    @Scheduled(cron = "0 */5 * * * *")
    public void tradeEnginBackup() {
        System.out.println();
        System.out.println("----------------------------------------start to backup trade engine----------------------------------------");
        TradeEngineBackupDO tradeEngineBackup = new TradeEngineBackupDO();
        tradeEngineBackup.sequenceId = tradeEnginService.getLastSequenceId();
        tradeEngineBackup.assets = getAssetBackup();
        tradeEngineBackup.orders = getOrderBackup();
        tradeEngineBackup.match = getMatchBackup();
        String backupJson = JsonUtil.writeJsonWithPrettyPrint(tradeEngineBackup);
        FileUtil.saveStringToLocal(backupJson, backupPath);
        log.info("备份结束...");
    }

    /**
     * 获取资产备份:
     * {
     *     用户ID1: [USD可用, USD冻结, BTC可用, BTC冻结],
     *     用户ID2: [USD可用, USD冻结, BTC可用, BTC冻结],
     *     ...
     * }
     * */
    private Map<Long, BigDecimal[]> getAssetBackup() {
        log.info("正在备份资产系统...");
        ConcurrentMap<Long, ConcurrentMap<AssetType, Asset>> activeAssets = assetService.getUserAssets();
        Map<Long, BigDecimal[]> backupMap = new HashMap<>();
        for (Map.Entry<Long, ConcurrentMap<AssetType, Asset>> userEntry : activeAssets.entrySet()) {
            BigDecimal[] assetBackup = new BigDecimal[4];
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetType, Asset> userAssets = userEntry.getValue();
            for (Map.Entry<AssetType, Asset> assetEntry : userAssets.entrySet()) {
                AssetType type = assetEntry.getKey();
                Asset asset = assetEntry.getValue();
                if (type == AssetType.USD) {
                    assetBackup[0] = asset.getAvailable();
                    assetBackup[1] = asset.getFrozen();
                } else if (type == AssetType.BTC) {
                    assetBackup[2] = asset.getAvailable();
                    assetBackup[3] = asset.getFrozen();
                }
                backupMap.put(userId, assetBackup);
            }
        }
        if (backupMap.isEmpty()) {
            return new HashMap<>();
        }
        return backupMap;
    }

    /**
     * 获取订单备份:
     * [
     *     { "id": 10012207, "sequenceId": 1001, "price": 20901, ...},
     *     { "id": 10022207, "sequenceId": 1002, "price": 20902, ...},
     *     ...
     * ]
     * */
    private List<OrderEntity> getOrderBackup() {
        log.info("正在备份订单资产系统...");
        ConcurrentMap<Long, OrderEntity> activeOrders = orderService.getActiveOrders();
        return activeOrders.values().stream().toList();
    }

    /**
     * 获取订单撮合备份：
     * {
     *     "BUY": [10012207, 10022207, ...],
     *     "SELL": [...],
     *     "marketPrice": 20901
     * }
     * */
    private TradeEngineBackupDO.MatchBackup getMatchBackup() {
        log.info("正在备份撮合系统...");
        TradeEngineBackupDO.MatchBackup matchBackup = new TradeEngineBackupDO.MatchBackup();
        // BUY
        OrderBook BUY = matchService.getOrderBook(Direction.BUY);
        List<OrderEntity> buyOrders = BUY.book.values().stream().toList();
        matchBackup.BUY = new ArrayList<>(buyOrders.stream().map(o1 -> o1.id).toList());
        //SELL
        OrderBook SELL = matchService.getOrderBook(Direction.SELL);
        List<OrderEntity> sellOrders = SELL.book.values().stream().toList();
        matchBackup.SELL = new ArrayList<>(sellOrders.stream().map(o -> o.id).toList());
        //marketPrice
        matchBackup.marketPrice = matchService.getMarketPrice();
        return matchBackup;
    }
}
