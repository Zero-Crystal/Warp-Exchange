package com.exchange.trade.engin.asset;

import com.exchange.common.enums.AssetType;
import com.exchange.trade.engin.asset.entity.Asset;
import com.exchange.trade.engin.asset.entity.TransferType;
import com.exchange.trade.engin.asset.service.AssetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class AssetServiceTest {
    private final Long BASE_ACCOUNT = 0000L;

    private final Long ACCOUNT_A = 1000L;

    private final Long ACCOUNT_B = 2000l;

    private AssetServiceImpl assetServiceImpl1;

    @BeforeEach
    public void initTest() {
        assetServiceImpl1 = new AssetServiceImpl();
        //init account A USD
        assetServiceImpl1.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
                AssetType.USD, BigDecimal.valueOf(10000), false);
        //init account A BTC
        assetServiceImpl1.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_A,
                AssetType.BTC, BigDecimal.valueOf(12000), false);

        //init account B USD
        assetServiceImpl1.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_B,
                AssetType.USD, BigDecimal.valueOf(30000), false);
        //init account B BTC
        assetServiceImpl1.baseTransfer(TransferType.AVAILABLE_TO_AVAILABLE, BASE_ACCOUNT, ACCOUNT_B,
                AssetType.BTC, BigDecimal.valueOf(50000), false);
    }

    @AfterEach
    public void userAssets() {
        ConcurrentMap<Long, ConcurrentMap<AssetType, Asset>> userAssets = assetServiceImpl1.getUserAssets();
        for (Long accountId : userAssets.keySet()) {
            Map<AssetType, Asset> userAsset = assetServiceImpl1.getAssets(accountId);
            if (userAsset.containsKey(AssetType.USD)) {
                Asset asset = userAsset.get(AssetType.USD);
                System.out.println("account:" + accountId + " - " + " USD - " + asset.toString());
            }
            if (userAsset.containsKey(AssetType.BTC)) {
                Asset asset = userAsset.get(AssetType.BTC);
                System.out.println("account:" + accountId + " - " + " BTC - " + asset.toString());
            }
        }
        System.out.println("=========================测试结束=========================");
    }

    @Test
    public void transferTest() {
        assetServiceImpl1.assetTransfer(ACCOUNT_A, ACCOUNT_B, AssetType.BTC, BigDecimal.valueOf(2200));
        System.out.println("=========================转账结束=========================");
    }

    @Test
    public void freezeAssetTest() {
        assetServiceImpl1.assetFreeze(ACCOUNT_A, AssetType.BTC, BigDecimal.valueOf(1000));
        System.out.println("=========================冻结结束=========================");
    }

    @Test
    public void unFreezeAssetTest() {
        assetServiceImpl1.assetFreeze(ACCOUNT_B, AssetType.USD, BigDecimal.valueOf(3500));
        assetServiceImpl1.assetUnFreeze(ACCOUNT_B, AssetType.USD, BigDecimal.valueOf(3500));
        System.out.println("=========================解冻结束=========================");
    }
}
