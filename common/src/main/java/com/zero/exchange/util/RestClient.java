package com.zero.exchange.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.api.ApiResult;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RestClient {

    private final Logger log = LoggerFactory.getLogger(RestClient.class);

    private String endPoint;

    private String host;

    private ObjectMapper objectMapper;

    private OkHttpClient client;

    public RestClient(String endPoint, String host, ObjectMapper objectMapper, OkHttpClient client) {
        this.endPoint = endPoint;
        this.host = host;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public static class Build {
        private final Logger log = LoggerFactory.getLogger(Build.class);

        private String scheme;

        private String host;

        private int port;

        private int connectTimeout = 3;
        private int readTimeout = 3;
        private int keepAliveTime = 30;

        /**
         * Create builder with api endpoint. e.g. "http://localhost:8080". NOTE: do not append any PATH.
         *
         * @param apiEndpoint The api endpoint.
         */
        public Build(String apiEndpoint) {
            try {
                log.info("start to build restClient from api endpoint: {}", apiEndpoint);
                URI uri = new URI(apiEndpoint);
                if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) {
                    throw new IllegalArgumentException("API Endpoint 不合法: " + apiEndpoint);
                }
                if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                    throw new IllegalArgumentException("API Endpoint 不能包含 path 内容");
                }
                this.scheme = uri.getScheme();
                this.host = uri.getHost();
                this.port = uri.getPort();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        public Build setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Build setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Build setKeepAliveTime(int keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public RestClient build(ObjectMapper objectMapper) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(0, keepAliveTime, TimeUnit.SECONDS))
                    .retryOnConnectionFailure(false)
                    .build();
            String endpoint = this.scheme + "://" + this.host;
            if (port != (-1)) {
                endpoint += ":" + port;
            }
            return new RestClient(endpoint, host, objectMapper, client);
        }
    }

    public <T> T get(Class<T> clazz, String path, String authHeader, Map<String, String> query) {
        Objects.requireNonNull(clazz);
        return request(clazz, null, "GET", path, authHeader, query, null);
    }

    public <T> T get(TypeReference<T> ref, String path, String authHeader, Map<String, String> query) {
        Objects.requireNonNull(ref);
        return request(null, ref, "GET", path, authHeader, query, null);
    }

    public <T> T post(TypeReference<T> ref, String path, String authHeader, Object body) {
        Objects.requireNonNull(ref);
        return request(null, ref, "POST", path, authHeader, null, body);
    }

    public <T> T post(Class<T> clazz, String path, String authHeader, Object body) {
        Objects.requireNonNull(clazz);
        return request(clazz, null, "POST", path, authHeader, null, body);
    }

    private <T> T request(Class<T> clazz, TypeReference<T> ref, String method, String path, String authHeader,
                          Map<String, String> query, Object body) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("非法 http 请求 path: " + path);
        }

        // parse query map
        String queryStr = null;
        if (query != null) {
            List<String> queryList = new ArrayList<>();
            for (Map.Entry<String, String> entry : query.entrySet()) {
                queryList.add(entry.getKey() + "=" + entry.getValue());
            }
            queryStr = String.join("&", queryList);
        }

        // parse url
        StringBuilder urlBuilder = new StringBuilder(64).append(endPoint).append(path);
        if (queryStr != null) {
            urlBuilder.append("?").append(queryStr);
        }
        final String url = urlBuilder.toString();

        // parse json body
        String jsonBody;
        try {
            jsonBody = body == null ? "" : (body instanceof String ? (String) body : objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Request.Builder builder = new Request.Builder().url(url);
        if (authHeader != null) {
            builder.addHeader("Authorization", authHeader);
        }

        if ("POST".equals(method)) {
            builder.post(RequestBody.create(jsonBody, JSON));
        }

        Request request = builder.build();
        try {
            return execute(clazz, ref, request);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T execute(Class<T> clazz, TypeReference<T> ref, Request request) throws IOException {
        try(Response response = this.client.newCall(request).execute()) {
            if (response.code() == 200) {
                try (ResponseBody body = response.body()) {
                    String json = body.string();
                    if (clazz == null) {
                        return objectMapper.readValue(json, ref);
                    }
                    if (clazz == String.class) {
                        return (T) json;
                    }
                    return objectMapper.readValue(json, clazz);
                }
            } else {
                try(ResponseBody body = response.body()) {
                    String json = body.string();
                    ApiResult error = objectMapper.readValue(json, ApiResult.class);
                    if (error.getMessage() == null || error.getMessage().isEmpty()) {
                        throw UNKNOWN_ERROR;
                    }
                    throw new ApiException(error.getCode(), objectMapper.writeValueAsString(error.getData()), error.getMessage());
                }
            }
        }
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final ApiException UNKNOWN_ERROR = new ApiException(ApiError.INTERNAL_SERVER_ERROR, "api", "Api failed without error code.");
}
