package com.zero.exchange.service;

import com.zero.exchange.model.AuthToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CookieService {

    long getExpressInSecond();

    AuthToken findTokenInCookie(HttpServletRequest request);

    void setSessionCookie(HttpServletRequest request, HttpServletResponse response, AuthToken token);

    void deleteSessionCookie(HttpServletRequest request, HttpServletResponse response);
}
