package com.zero.exchange.message;

import com.zero.exchange.api.redis.ApiError;
import com.zero.exchange.api.ApiErrorResponse;

public class ApiResultMessage extends AbstractMessage {

    private static ApiErrorResponse CREATE_ORDER_FAILED = new ApiErrorResponse(ApiError.NO_ENOUGH_ASSET,
            "not enough asset", null);

    private static ApiErrorResponse CANCEL_ORDER_FAILED = new ApiErrorResponse(ApiError.ORDER_NOT_FOUND,
            "order not found", null);

    public ApiErrorResponse error;

    public Object result;

    public static ApiResultMessage createOrderFailed(String refId, long ts) {
        ApiResultMessage apiResultMessage = new ApiResultMessage();
        apiResultMessage.error = CREATE_ORDER_FAILED;
        apiResultMessage.refId = refId;
        apiResultMessage.createAt = ts;
        return apiResultMessage;
    }

    public static ApiResultMessage cancelOrderFailed(String refId, long ts) {
        ApiResultMessage apiResultMessage = new ApiResultMessage();
        apiResultMessage.error = CANCEL_ORDER_FAILED;
        apiResultMessage.refId = refId;
        apiResultMessage.createAt = ts;
        return apiResultMessage;
    }

    public static ApiResultMessage orderSuccess(String refId, long ts, Object result) {
        ApiResultMessage apiResultMessage = new ApiResultMessage();
        apiResultMessage.refId = refId;
        apiResultMessage.createAt = ts;
        apiResultMessage.result = result;
        return apiResultMessage;
    }
}
