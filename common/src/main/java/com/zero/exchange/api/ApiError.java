package com.zero.exchange.api;

public enum ApiError {
    PARAMETER_INVALID(-100, "参数异常"),

    AUTH_SIGNIN_REQUIRE(-101, "请先登录系统"),

    AUTH_SIGNIN_FAILED(-102, "登录失败"),

    USER_CAN_NOT_SIGNIN(-103, "用户无法登录"),

    NO_ENOUGH_ASSET(-104, "资金不足"),

    ORDER_NOT_FOUND(-105, "未查询到该订单"),

    OPERATION_TIMEOUT(-106, "操作超时"),

    INTERNAL_SERVER_ERROR(-107, "服务器异常");

    private int code;

    private String message;

    ApiError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
