package com.zero.exchange.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.context.UserContext;
import com.zero.exchange.model.AuthToken;
import com.zero.exchange.support.AbstractFilter;
import com.zero.exchange.util.RestClient;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProxyFilterRegisterBean extends FilterRegistrationBean<Filter> {

    @Autowired
    private RestClient restClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("#{exchangeConfiguration.hmacKey}")
    private String hmacKey;

    @PostConstruct
    public void init() {
        ProxyFilter proxyFilter = new ProxyFilter();
        setFilter(proxyFilter);
        addUrlPatterns("/api/*");
        setName(proxyFilter.getClass().getSimpleName());
        setOrder(200);
    }

    class ProxyFilter extends AbstractFilter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            Long userId = UserContext.getUserId();
            String path = request.getRequestURI();
            log.info("{} {}: -------------> {}", userId, request.getMethod(), path);
            proxyForward(userId, request, response);
        }

        public void proxyForward(Long userId, HttpServletRequest request, HttpServletResponse response) throws IOException {
            String authToken = null;
            if (userId != null) {
                AuthToken auth = new AuthToken(userId, System.currentTimeMillis() + 60_000);
                authToken = "Bearer " + auth.toSecureString(hmacKey);
            }
            try {
                String responseJson = null;
                if ("GET".equals(request.getMethod())) {
                    Map<String, String[]> params = request.getParameterMap();
                    Map<String, String> queryMap = convertParams(params);
                    responseJson = restClient.get(String.class, request.getRequestURI(), authToken, queryMap);
                } else if ("POST".equals(request.getMethod())) {
                    responseJson = restClient.post(String.class, request.getRequestURI(), authToken, readBody(request));
                }
                response.setContentType("application/json;charset=utf-8");
                PrintWriter pw = response.getWriter();
                pw.write(responseJson);
                pw.flush();
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
                writeApiException(response, e);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                ApiException exception = new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, e.getMessage());
                writeApiException(response, exception);
            }
        }

        private Map<String, String> convertParams(Map<String, String[]> params) {
            Map<String, String> paramMap = new HashMap<>();
            params.forEach((key, values) -> {
                paramMap.put(key, values[0]);
                log.info("{} -> {}", key, values);
            });
            return paramMap;
        }

        private String readBody(HttpServletRequest request) throws IOException {
            StringBuilder sb = new StringBuilder(2048);
            char[] buffer = new char[256];
            BufferedReader reader = request.getReader();
            for (;;) {
                int n = reader.read(buffer);
                if (n == -1) {
                    break;
                }
                sb.append(buffer, 0, n);
            }
            return sb.toString();
        }

        private void writeApiException(HttpServletResponse response, ApiException e) throws IOException {
            response.setContentType("application/json;charset=utf-8");
            PrintWriter pw = response.getWriter();
            pw.write(objectMapper.writeValueAsString(e.errorResult));
            pw.flush();
        }
    }
}
