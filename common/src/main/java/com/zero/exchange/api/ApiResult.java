package com.zero.exchange.api;

import lombok.Data;

@Data
public class ApiResult {

    private Integer code;

    private String message;

    private Object data;

    public static ApiResult failure(String message) {
        return failure(-100, message);
    }

    public static ApiResult failure(Integer code, String message) {
        ApiResult apiResult = new ApiResult();
        apiResult.setCode(code);
        apiResult.setMessage(message);
        return apiResult;
    }

    public static ApiResult success(Object data) {
        ApiResult apiResult = new ApiResult();
        apiResult.setCode(200);
        apiResult.setData(data);
        return apiResult;
    }
}
