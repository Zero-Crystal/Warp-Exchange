package com.zero.exchange.api;

public class ApiException extends RuntimeException {
    public final ApiResult errorResult;

    public ApiException(ApiError error) {
        super(error.toString());
        errorResult = ApiResult.failure(error);
    }

    public ApiException(ApiError error, String data) {
        super(error.toString());
        errorResult = ApiResult.failure(error, data);
    }

    public ApiException(ApiError error, String data, String message) {
        super(error.toString());
        errorResult = ApiResult.failure(error.getCode(), message, data);
    }

    public ApiException(Integer error, String data, String message) {
        super(error.toString());
        errorResult = ApiResult.failure(error, message, data);
    }
}
