package com.exchange.common.message.event;

import com.exchange.common.enums.Direction;

import java.math.BigDecimal;

public class OrderRequestEvent extends AbstractEvent {

    /**
     * 用户id
     * */
    public Long accountId;

    /**
     * 交易方向
     * */
    public Direction direction;

    /**
     * 交易价格
     * */
    public BigDecimal price;

    /**
     * 交易数量
     * */
    public BigDecimal quantity;

    @Override
    public String toString() {
        return "OrderRequestEvent [" + "refId: '" + refId + ", createAt: " + createAt + ", sequenceId: " + sequenceId +
                ", previousId: " + previousId + ", uniqueId: " + uniqueId + ", accountId: " + accountId +
                ", direction: " + direction + ", price: " + price + ", quantity: " + quantity + ']';
    }
}
