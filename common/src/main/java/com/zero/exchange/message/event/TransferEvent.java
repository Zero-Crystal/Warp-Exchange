package com.zero.exchange.message.event;

import com.zero.exchange.enums.AssetType;

import java.math.BigDecimal;

public class TransferEvent extends AbstractEvent {

    /**
     * 交易发起人id
     * */
    public Long fromUserId;

    /**
     * 交易对象id
     * */
    public Long toUserId;

    /**
     * 交易货物类型
     * */
    public AssetType assetType;

    /**
     * 交易金额
     * */
    public BigDecimal amount;

    public boolean sufficient;

    @Override
    public String toString() {
        return "TransferEvent [" + "refId: '" + refId + ", sequenceId: " + sequenceId + ", previousId: " + previousId +
                ", uniqueId: " + uniqueId + ", fromUserId: " + fromUserId + ", toUserId: " + toUserId +
                ", assetType: " + assetType + ", amount: " + amount + ", sufficient: " + sufficient + ']';
    }
}
