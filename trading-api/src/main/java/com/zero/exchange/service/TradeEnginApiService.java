package com.zero.exchange.service;

import com.zero.exchange.api.ApiResult;

import java.io.IOException;

public interface TradeEnginApiService {

    /**
     * 向 TradeEngin 发送 GET 请求
     *
     * @param url
     * @return String
     * */
    ApiResult get(String url) throws IOException;
}
