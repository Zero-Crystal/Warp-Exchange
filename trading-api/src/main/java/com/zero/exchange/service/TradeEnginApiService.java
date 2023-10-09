package com.zero.exchange.service;

import java.io.IOException;

public interface TradeEnginApiService {

    /**
     * 向 TradeEngin 发送 GET 请求
     *
     * @param url
     * @return String
     * */
    String get(String url) throws IOException;
}
