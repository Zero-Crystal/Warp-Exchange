package com.exchange.trade.engin.asset.service;

import com.exchange.common.enums.AssetType;
import com.exchange.trade.engin.asset.entity.Asset;
import com.exchange.trade.engin.asset.entity.TransferType;

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
     * @param fromAccountId 转账方
     * @param toAccountId 收款方
     * @param assetType 转账资产类型
     * @param amount 转账金额
     * */
    void assetTransfer(Long fromAccountId, Long toAccountId, AssetType assetType, BigDecimal amount);

    /**
     * 冻结资产
     *
     * @param accountId 冻结用户
     * @param assetType 冻结资产类型
     * @param amount 冻结金额
     * */
    boolean assetFreeze(Long accountId, AssetType assetType, BigDecimal amount);

    /**
     * 解冻资产
     *
     * @param accountId 解冻用户
     * @param assetType 解冻资产类型
     * @param amount 解冻金额
     * */
    void assetUnFreeze(Long accountId, AssetType assetType, BigDecimal amount);

    /**
     * 转账操作
     *
     * @param transferType 转账类型
     * @param fromAccountId 转账方
     * @param toAccountId 收款方
     * @param assetType 资产类型
     * @param amount 转账金额
     * @param checkBalance 是否需要检查余额
     * */
    boolean transfer(TransferType transferType, Long fromAccountId, Long toAccountId,
                     AssetType assetType, BigDecimal amount, boolean checkBalance);
}
