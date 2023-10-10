package com.zero.exchange.api;

public class ApiException extends RuntimeException {
    public final ApiResult errorResult;

    public ApiException(ApiError error) {
        super(error.toString());
        errorResult = ApiResult.failure(error.getCode(), error.getMessage());
    }

    public ApiException(ApiError error, String data) {
        super(error.toString());
        errorResult = ApiResult.failure(error.getCode(), error.getMessage(), data);
    }

    public ApiException(ApiError error, String data, String message) {
        super(error.toString());
        errorResult = ApiResult.failure(error.getCode(), message, data);
    }
}
