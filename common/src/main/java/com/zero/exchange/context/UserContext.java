package com.zero.exchange.context;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;

/**
 * Holds user context in thread-local.
 */
public class UserContext implements AutoCloseable {

    static final ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();

    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }

    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }

    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRE, "Need sign in first");
        }
        return userId;
    }

    @Override
    public void close() {
        THREAD_LOCAL_CTX.remove();
    }
}
