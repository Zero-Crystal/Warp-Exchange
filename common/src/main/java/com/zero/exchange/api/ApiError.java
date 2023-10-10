package com.zero.exchange.api;

public enum ApiError {
    PARAMETER_INVALID(-101, "参数异常"),

    AUTH_SIGNIN_REQUIRE(-102, "请先登录系统"),

    AUTH_SIGNIN_FAILED(-103, "登录失败"),

    USER_CAN_NOT_SIGNIN(-104, "用户无法登录"),

    NO_ENOUGH_ASSET(-105, "资金不足"),

    ORDER_NOT_FOUND(-106, "未查询到该订单"),

    OPERATION_TIMEOUT(-107, "操作超时"),

    INTERNAL_SERVER_ERROR(-108, "服务器异常");

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
