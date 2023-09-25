package com.zero.exchange.api;

public record ApiErrorResponse(ApiError apiError, String message, String data) {
}
