package com.exchange.trade.engin.asset.service;

import com.exchange.common.enums.AssetType;
import com.exchange.common.support.LoggerSupport;
import com.exchange.trade.engin.asset.entity.Asset;
import com.exchange.trade.engin.asset.entity.TransferType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AssetServiceImpl extends LoggerSupport implements AssetService {

    private final ConcurrentMap<Long, ConcurrentMap<AssetType, Asset>> userAssetsMap = new ConcurrentHashMap<>();

    @Override
    public Asset getAsset(Long userId, AssetType assetType) {
        ConcurrentMap<AssetType, Asset> assets = userAssetsMap.get(userId);
        if (assets == null) {
            return null;
        }
        return assets.get(assetType);
    }

    @Override
    public Map<AssetType, Asset> getAssets(Long userId) {
        Map<AssetType, Asset> assets = userAssetsMap.get(userId);
        if (assets == null) {
            return Map.of();
        }
        return assets;
    }

    @Override
    public ConcurrentMap<Long, ConcurrentMap<AssetType, Asset>> getUserAssets() {
        return userAssetsMap;
    }

    @Override
    public void assetTransfer(Long fromAccountId, Long toAccountId, AssetType assetType, BigDecimal amount) {
        if (!transfer(TransferType.AVAILABLE_TO_AVAILABLE, fromAccountId, toAccountId, assetType, amount, true)) {
            throw new RuntimeException("account " +  + fromAccountId + " transfer to account" + toAccountId
                    + " failed, amount=" + amount + ", asset type=" + assetType);
        }
        if (log.isDebugEnabled()) {
            log.info("account=[{}] transfer to user=[{}], amount={}, asset type={}",
                    fromAccountId, toAccountId, amount, assetType);
        }
    }

    @Override
    public boolean assetFreeze(Long accountId, AssetType assetType, BigDecimal amount) {
        boolean isFreezeOk = transfer(TransferType.AVAILABLE_TO_FROZEN, accountId, accountId, assetType, amount, true);
        if (isFreezeOk && log.isDebugEnabled()) {
            log.debug("account=[{}] frozen asset, amount={}, asset type={}", accountId, amount, assetType);
        }
        return isFreezeOk;
    }

    @Override
    public void assetUnFreeze(Long accountId, AssetType assetType, BigDecimal amount) {
        if (!transfer(TransferType.FROZEN_TO_AVAILABLE, accountId, accountId, assetType, amount, true)) {
            throw new RuntimeException("account " + accountId + " unfrozen asset failed, amount=" + amount + ", asset type=" + assetType);
        }
        if (log.isDebugEnabled()) {
            log.debug("account=[{}] unfrozen asset, amount={}, asset type={}", accountId, amount, assetType);
        }
    }

    @Override
    public boolean transfer(TransferType transferType, Long fromAccountId, Long toAccountId,
                            AssetType assetType, BigDecimal amount, boolean checkBalance) {
        //检查转账金额
        if (amount.signum() == 0) {
            return true;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount can not small than 0");
        }
        Asset fromAsset = getAsset(fromAccountId, assetType);
        if (fromAsset == null) {
            fromAsset = initAsset(fromAccountId, assetType);
        }
        Asset toAsset = getAsset(toAccountId, assetType);
        if (toAsset == null) {
            toAsset = initAsset(toAccountId, assetType);
        }
        return switch (transferType) {
            case AVAILABLE_TO_AVAILABLE -> {
                //检查转账方可用账户余额是否可用
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    log.info("余额不可用, availableAsset={}, amount={}", fromAsset.available, amount);
                    yield false;
                }
                //转账
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                //检查转账方可用账户余额是否可用
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    log.info("余额不可用, availableAsset={}, amount={}", fromAsset.available, amount);
                    yield false;
                }
                //冻结
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                //检查转账方冻结账户是否可用
                if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    log.info("余额不可用, frozenAsset={}, amount={}", fromAsset.frozen, amount);
                    yield false;
                }
                //解冻
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("wrong transfer type: " + transferType);
            }
        };
    }

    /**
     * 初始化一个新账户
     * @param accountId 账户id
     * @param assetType 资产类型
     * */
    private Asset initAsset(Long accountId, AssetType assetType) {
        ConcurrentMap<AssetType, Asset> assets = userAssetsMap.get(accountId);
        if (assets == null) {
            assets = new ConcurrentHashMap<>();
            userAssetsMap.put(accountId, assets);
        }
        Asset zeroAsset = new Asset();
        assets.put(assetType, zeroAsset);
        return zeroAsset;
    }
}
