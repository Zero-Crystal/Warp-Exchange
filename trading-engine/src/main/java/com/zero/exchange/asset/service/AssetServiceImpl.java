package com.zero.exchange.asset.service;

import com.zero.exchange.enums.AssetType;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.asset.entity.Asset;
import com.zero.exchange.asset.entity.TransferType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    public void assetTransfer(Long fromuserId, Long touserId, AssetType assetType, BigDecimal amount) {
        if (!baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, fromuserId, touserId, assetType, amount, true)) {
            throw new RuntimeException("account " +  + fromuserId + " transfer to account" + touserId
                    + " failed, amount=" + amount + ", asset type=" + assetType);
        }
        if (log.isDebugEnabled()) {
            log.info("account=[{}] transfer to user=[{}], amount={}, asset type={}",
                    fromuserId, touserId, amount, assetType);
        }
    }

    @Override
    public boolean assetFreeze(Long userId, AssetType assetType, BigDecimal amount) {
        boolean isFreezeOk = baseTransfer(TransferType.AVAILABLE_TO_FROZEN, userId, userId, assetType, amount, true);
        if (isFreezeOk && log.isDebugEnabled()) {
            log.debug("account=[{}] frozen asset, amount={}, asset type={}", userId, amount, assetType);
        }
        return isFreezeOk;
    }

    @Override
    public void assetUnFreeze(Long userId, AssetType assetType, BigDecimal amount) {
        if (!baseTransfer(TransferType.FROZEN_TO_AVAILABLE, userId, userId, assetType, amount, true)) {
            throw new RuntimeException("account " + userId + " unfrozen asset failed, amount=" + amount + ", asset type=" + assetType);
        }
        if (log.isDebugEnabled()) {
            log.debug("account=[{}] unfrozen asset, amount={}, asset type={}", userId, amount, assetType);
        }
    }

    @Override
    public boolean baseTransfer(TransferType transferType, Long fromuserId, Long touserId,
                                AssetType assetType, BigDecimal amount, boolean checkBalance) {
        //检查转账金额
        if (amount.signum() == 0) {
            return true;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount can not small than 0");
        }
        Asset fromAsset = getAsset(fromuserId, assetType);
        if (fromAsset == null) {
            fromAsset = initAsset(fromuserId, assetType);
        }
        Asset toAsset = getAsset(touserId, assetType);
        if (toAsset == null) {
            toAsset = initAsset(touserId, assetType);
        }
        return switch (transferType) {
            case AVAILABLE_TO_AVAILABLE -> {
                //检查转账方可用账户余额是否可用
                if (checkBalance && fromAsset.getAvailable().compareTo(amount) < 0) {
                    log.info("余额不可用, availableAsset={}, amount={}", fromAsset.getAvailable(), amount);
                    yield false;
                }
                //转账
                fromAsset.setAvailable(fromAsset.getAvailable().subtract(amount));
                toAsset.setAvailable(toAsset.getAvailable().add(amount));
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                //检查转账方可用账户余额是否可用
                if (checkBalance && fromAsset.getAvailable().compareTo(amount) < 0) {
                    log.info("余额不可用, availableAsset={}, amount={}", fromAsset.getAvailable(), amount);
                    yield false;
                }
                //冻结
                fromAsset.setAvailable(fromAsset.getAvailable().subtract(amount));
                toAsset.setFrozen(toAsset.getFrozen().add(amount));
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                //检查转账方冻结账户是否可用
                if (checkBalance && fromAsset.getFrozen().compareTo(amount) < 0) {
                    log.info("余额不可用, frozenAsset={}, amount={}", fromAsset.getFrozen(), amount);
                    yield false;
                }
                //解冻
                fromAsset.setFrozen(fromAsset.getFrozen().subtract(amount));
                toAsset.setAvailable(toAsset.getAvailable().add(amount));
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("wrong transfer type: " + transferType);
            }
        };
    }

    @Override
    public void debug() {
        System.out.println();
        System.out.println("----------------------------asset----------------------------");
        List<Long> userIds = new ArrayList<>(userAssetsMap.keySet());
        Collections.sort(userIds);
        for (Long userId : userIds) {
            System.out.println("----> userId: " + userId);
            Map<AssetType, Asset> userAsset = userAssetsMap.get(userId);
            List<AssetType> assetTypes = new ArrayList<>(userAsset.keySet());
            Collections.sort(assetTypes);
            for (AssetType type : assetTypes) {
                System.out.println("    " + type + ": " + userAsset.get(type));
            }
        }
    }

    /**
     * 初始化一个新账户
     * @param userId 账户id
     * @param assetType 资产类型
     * */
    private Asset initAsset(Long userId, AssetType assetType) {
        ConcurrentMap<AssetType, Asset> assets = userAssetsMap.get(userId);
        if (assets == null) {
            assets = new ConcurrentHashMap<>();
            userAssetsMap.put(userId, assets);
        }
        Asset zeroAsset = new Asset();
        assets.put(assetType, zeroAsset);
        return zeroAsset;
    }
}
