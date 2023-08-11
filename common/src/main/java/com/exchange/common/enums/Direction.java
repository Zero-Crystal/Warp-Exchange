package com.exchange.common.enums;

/**
 * 订单方向
 * @author zero
 * @createTime 2023/07/26
 * */
public enum Direction {
    BUY(0),

    SELL(1);


    private final int value;

    Direction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Get negate direction.
     */
    public Direction negate() {
        return this == BUY ? SELL : BUY;
    }

    public static Direction of(int intValue) {
        if (intValue == 1) {
            return BUY;
        }
        if (intValue == 0) {
            return SELL;
        }
        throw new IllegalArgumentException("Invalid Direction value.");
    }
}
