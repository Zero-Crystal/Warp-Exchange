package com.zero.exchange.api;

import com.zero.exchange.api.redis.ApiError;

public record ApiErrorResponse(ApiError apiError, String message, String data) {
}
