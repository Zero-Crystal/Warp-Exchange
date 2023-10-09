package com.zero.exchange.api.redis;

import com.zero.exchange.api.ApiErrorResponse;

public class ApiException extends RuntimeException {
    public final ApiErrorResponse error;

    public ApiException(ApiError error) {
        super(error.toString());
        this.error = new ApiErrorResponse(error, "", null);
    }

    public ApiException(ApiError error, String data) {
        super(error.toString());
        this.error = new ApiErrorResponse(error, "", data);
    }

    public ApiException(ApiError error, String data, String message) {
        super(error.toString());
        this.error = new ApiErrorResponse(error, message, data);
    }
}
