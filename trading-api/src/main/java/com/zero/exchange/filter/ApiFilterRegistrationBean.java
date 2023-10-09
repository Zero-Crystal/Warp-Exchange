package com.zero.exchange.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zero.exchange.api.redis.ApiError;
import com.zero.exchange.api.redis.ApiException;
import com.zero.exchange.model.AuthToken;
import com.zero.exchange.context.UserContext;
import com.zero.exchange.entity.ui.ApiAuthEntity;
import com.zero.exchange.entity.ui.UserProfileEntity;
import com.zero.exchange.support.AbstractFilter;
import com.zero.exchange.user.UserService;
import com.zero.exchange.util.HashUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class ApiFilterRegistrationBean extends FilterRegistrationBean<Filter> {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("#{exchangeConfiguration.hmacKey}")
    private String hmacKey;

    @PostConstruct
    public void init() {
        ApiFilter filter = new ApiFilter();
        setFilter(filter);
        addUrlPatterns("/api/*");
        setName(filter.getClass().getSimpleName());
        setOrder(100);
    }

    public class ApiFilter extends AbstractFilter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            Long userId = null;
            try {
                userId = parseUser(request);
            } catch (ApiException e) {
                sendErrorResponse(response, e);
            }
            if (userId == null) {
                // 匿名用户
                filterChain.doFilter(request, response);
            } else {
                try (UserContext ctx = new UserContext(userId)) {
                    filterChain.doFilter(request, response);
                }
            }
        }

        private Long parseUser(HttpServletRequest request) {
            // Authorization 认证
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                return parseUserFromAuthorization(auth);
            }
            // API-Key 认证
            String apiKey = request.getHeader("Api-Key");
            String apiSignature = request.getHeader("Api-Signature");
            if (apiKey != null && apiSignature != null) {
                return parseUserFromApiKey(apiKey, apiSignature, request);
            }
            return null;
        }

        /**
         * 签名认证:
         * Basic base64(email:password)
         * Bearer token
         *
         * @param auth 签名信息
         * @return Long userId
         * */
        private Long parseUserFromAuthorization(String auth) {
            // Basic签名
            if (auth.startsWith("Basic ")) {
                String author = new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8);
                int pos = author.indexOf(":");
                if (pos < 1) {
                    throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid email and password.");
                }
                String email = author.substring(0, pos);
                String password = author.substring(pos + 1);
                UserProfileEntity profile = userService.signIn(email, password);
                Long userId = profile.userId;
                if (log.isDebugEnabled()) {
                    log.debug("parse from Basic authorization: {}", userId);
                }
                return userId;
            }
            // Bearer签名
            if (auth.startsWith("Bearer ")) {
                AuthToken token = AuthToken.fromSecureString(auth.substring(7), hmacKey);
                if (token.isExpired()) {
                    return null;
                }
                if (log.isDebugEnabled()) {
                    log.debug("parse from Bearer authorization: {}", token.userId());
                }
                return token.userId();
            }
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid Authorization header.");
        }

        /**
         * api-key、api-secret 认证
         *
         * @param apiKey
         * @param apiSignature
         * @param request
         * @return Long userId
         * */
        private Long parseUserFromApiKey(String apiKey, String apiSignature, HttpServletRequest request) {
            // 验证API-Key, API-Secret并返回userId
            ApiAuthEntity apiAuthEntity = userService.getUserApiAuthByKey(apiKey);
            if (!apiSignature.equals(HashUtil.hmacSha256(apiAuthEntity.apiSecret, apiKey))) {
                return null;
            }
            long reqTime = Long.parseLong(request.getHeader("API-Timestamp"));
            if (reqTime > apiAuthEntity.expiresAt) {
                throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "user[" + apiAuthEntity.userId + "] api-secret is expired");
            }
            return apiAuthEntity.userId;
        }

        private void sendErrorResponse(HttpServletResponse response, ApiException e) throws IOException {
            response.sendError(400);
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write(objectMapper.writeValueAsString(e.error));
            pw.flush();
        }
    }
}
