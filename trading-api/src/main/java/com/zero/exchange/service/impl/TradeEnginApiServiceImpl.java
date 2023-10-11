package com.zero.exchange.service.impl;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.service.TradeEnginApiService;
import com.zero.exchange.support.LoggerSupport;
import okhttp3.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class TradeEnginApiServiceImpl extends LoggerSupport implements TradeEnginApiService {

    @Value("${exchange.config.api-endpoints.trade-engin-api}")
    private String tradeEnginEndPoint;

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false).build();

    @Override
    public String get(String url) throws IOException {
        String reqUrl = tradeEnginEndPoint + url;
        log.info("GET --> {}", reqUrl);
        Request request = new Request.Builder()
                .url(reqUrl)
                .addHeader("Accept", "*/*")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                log.error("url[{}] 请求失败，code[{}]", tradeEnginEndPoint + url, response.code());
                throw new ApiException(ApiError.OPERATION_TIMEOUT, "服务器请求超时");
            }
            try (ResponseBody body = response.body()) {
                String result = body.string();
                if (Strings.isEmpty(result)) {
                    log.error("服务器请求成功，但返回值为空：{}", result);
                    throw new ApiException(ApiError.INTERNAL_SERVER_ERROR, "服务器返回为空");
                }
                log.info("    <-- {}", result);
                return result;
            }
        }
    }
}
