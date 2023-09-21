package com.exchange.common.message.event;

public class OrderCancelEvent extends AbstractEvent {

    /**
     * 用户id
     * */
    public Long accountId;

    /**
     * 订单id
     * */
    public Long orderId;

    @Override
    public String toString() {
        return "OrderCancelEvent [" + "refId: '" + refId + ", createAt: " + createAt + ", sequenceId: " + sequenceId +
                ", previousId: " + previousId + ", uniqueId: " + uniqueId + ", accountId: '" + accountId +
                ", orderId: " + orderId + ']';
    }
}
