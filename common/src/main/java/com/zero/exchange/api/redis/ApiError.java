package com.zero.exchange.api.redis;

public enum ApiError {
    PARAMETER_INVALID,

    AUTH_SIGNIN_REQUIRE,

    AUTH_SIGNIN_FAILED,

    USER_CAN_NOT_SIGNIN,

    NO_ENOUGH_ASSET,

    ORDER_NOT_FOUND,

    OPERATION_TIMEOUT,

    INTERNAL_SERVER_ERROR;
}
