package com.zero.exchange.service.impl;

import com.zero.exchange.model.AuthToken;
import com.zero.exchange.service.CookieService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.util.HttpUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieServiceImpl extends LoggerSupport implements CookieService {

    @Value("#{exchangeConfiguration.hmacKey}")
    private String hmacKey;

    @Value("#{exchangeConfiguration.sessionTimeout}")
    private Duration sessionTimeout;

    private final String SESSION_COOKIE = "_exsession_";

    @Override
    public long getExpressInSecond() {
        return sessionTimeout.toSeconds();
    }

    @Override
    public AuthToken findTokenInCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                String token = cookie.getValue();
                AuthToken authToken = AuthToken.fromSecureString(token, hmacKey);
                return authToken.isExpired() ? null : authToken;
            }
        }
        return null;
    }

    @Override
    public void setSessionCookie(HttpServletRequest request, HttpServletResponse response, AuthToken token) {
        String tokenString = token.toSecureString(hmacKey);
        Cookie cookie = new Cookie(SESSION_COOKIE, tokenString);
        cookie.setPath("/");
        // 设置cookie的最大存活时间
        cookie.setMaxAge(3600);
        // 当设置为true时，表示cookie只能通过HTTP请求传输，无法被客户端脚本访问，这有助于防止跨站点脚本攻击
        cookie.setHttpOnly(true);
        // 设置cookie是否只能通过HTTPS连接传输。当设置为true时，表示cookie只能在HTTPS连接中传输，无法在普通的HTTP连接中传输。
        cookie.setSecure(HttpUtil.isSecure(request));
        String host = request.getServerName();
        if (host != null) {
            // 当设置为一个有效的域名时，表示该cookie只能在该域名下有效；当设置为null时，表示该cookie在所有域名下有效。
            cookie.setDomain(host);
        }
        response.addCookie(cookie);
    }

    @Override
    public void deleteSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE, "-delete-");
        cookie.setPath("/");
        // 当设置为0时，表示cookie将立即过期；当设置为负数时，表示cookie将在浏览器关闭之后过期。
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        String host = request.getServerName();
        if (host != null && host.startsWith("www.")) {
            // set cookie for domain "domain.com":
            String domain = host.substring(4);
            cookie.setDomain(domain);
        }
        response.addCookie(cookie);
    }
}
