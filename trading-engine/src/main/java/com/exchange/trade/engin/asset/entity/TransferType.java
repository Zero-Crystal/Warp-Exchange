package com.exchange.trade.engin.asset.entity;

/**
 * 转账类型
 * @author zero
 * @creatTime 2023/07/21
 * */
public enum TransferType {
    //可用转可用
    AVAILABLE_TO_AVAILABLE,
    //可用转冻结
    AVAILABLE_TO_FROZEN,
    //冻结转可用
    FROZEN_TO_AVAILABLE;
}
