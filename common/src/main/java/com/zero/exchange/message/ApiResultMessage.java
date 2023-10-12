package com.zero.exchange.message;

import com.zero.exchange.api.ApiResult;
import com.zero.exchange.api.ApiError;

public class ApiResultMessage extends AbstractMessage {

    private static ApiResult CREATE_ORDER_FAILED = ApiResult.failure(ApiError.NO_ENOUGH_ASSET.getCode(),
            "not enough asset", null);

    private static ApiResult CANCEL_ORDER_FAILED = ApiResult.failure(ApiError.ORDER_NOT_FOUND.getCode(),
            "order not found", null);

    public ApiResult error;

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
