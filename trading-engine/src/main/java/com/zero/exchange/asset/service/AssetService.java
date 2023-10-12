package com.zero.exchange.asset.service;

import com.zero.exchange.enums.AssetType;
import com.zero.exchange.asset.model.Asset;
import com.zero.exchange.asset.model.TransferType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 资产服务
 * @author zero
 * @createTime 2023/07/24
 * */
public interface AssetService {

    /**
     * 获取用户资产
     *
     * @param userId
     * @param assetType
     * */
    Asset getAsset(Long userId, AssetType assetType);

    /**
     * 获取用户全部资产
     * */
    Map<AssetType, Asset> getAssets(Long userId);

    /**
     * 获取资产数据
     * */
    ConcurrentMap<Long, ConcurrentMap<AssetType, Asset>> getUserAssets();

    /**
     * 转账
     *
     * @param fromuserId 转账方
     * @param touserId 收款方
     * @param assetType 转账资产类型
     * @param amount 转账金额
     * */
    void assetTransfer(Long fromuserId, Long touserId, AssetType assetType, BigDecimal amount);

    /**
     * 冻结资产
     *
     * @param userId 冻结用户
     * @param assetType 冻结资产类型
     * @param amount 冻结金额
     * */
    boolean assetFreeze(Long userId, AssetType assetType, BigDecimal amount);

    /**
     * 解冻资产
     *
     * @param userId 解冻用户
     * @param assetType 解冻资产类型
     * @param amount 解冻金额
     * */
    void assetUnFreeze(Long userId, AssetType assetType, BigDecimal amount);

    /**
     * 转账操作
     *
     * @param transferType 转账类型
     * @param fromuserId 转账方
     * @param touserId 收款方
     * @param assetType 资产类型
     * @param amount 转账金额
     * @param checkBalance 是否需要检查余额
     * */
    boolean baseTransfer(TransferType transferType, Long fromuserId, Long touserId,
                         AssetType assetType, BigDecimal amount, boolean checkBalance);

    void debug();
}
