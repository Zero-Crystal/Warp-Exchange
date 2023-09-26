package com.zero.exchange.api.redis;

public record ApiErrorResponse(ApiError apiError, String message, String data) {
}
