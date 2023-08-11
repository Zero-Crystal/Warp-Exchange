package com.exchange.common.enums;

/**
 * 订单状态
 * @author zero
 * @createTime 2023/07/26
 * */
public enum OrderStatus {
    /**
     * 等待提交(unfilledQuantity == quantity)
     * */
    PENDING(false),

    /**
     * 完全成交 (unfilledQuantity = 0)
     * */
    FULLY_FILLED(true),

    /**
     * 部分成交 (quantity > unfilledQuantity > 0)
     * */
    PARTIAL_FILLED(false),

    /**
     * 部分成交后取消 (quantity > unfilledQuantity > 0)
     * */
    PARTIAL_CANCELLED(true),

    /**
     * 完全取消 (unfilledQuantity == quantity)
     */
    FULLY_CANCELLED(true);

    private final boolean isFinalStatus;

    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }

    public boolean isFinalStatus() {
        return isFinalStatus;
    }
}
