package com.zero.exchange.service.impl;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiResult;
import com.zero.exchange.service.TradeEnginApiService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.util.JsonUtil;
import okhttp3.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class TradeEnginApiServiceImpl extends LoggerSupport implements TradeEnginApiService {

    @Value("#{exchangeConfiguration.apiEndpoints.tradeEnginApi}")
    private String tradeEnginEndPoint;

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false).build();

    @Override
    public ApiResult get(String url) throws IOException {
        String reqUrl = tradeEnginEndPoint + url;
        Request request = new Request.Builder()
                .url(reqUrl)
                .addHeader("Accept", "*/*")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                log.error("url[{}] 请求失败，code[{}]", tradeEnginEndPoint + url, response.code());
                return ApiResult.failure(ApiError.OPERATION_TIMEOUT.getCode(), "服务器请求超时");
            }
            try (ResponseBody body = response.body()) {
                String result = body.string();
                if (Strings.isEmpty(result)) {
                    log.error("服务器请求成功，但返回值为空：{}", result);
                    return ApiResult.failure(ApiError.INTERNAL_SERVER_ERROR.getCode(), "服务器返回为空");
                }
                return JsonUtil.readJson(result, ApiResult.class);
            }
        }
    }
}
