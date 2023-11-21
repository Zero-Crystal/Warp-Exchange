package com.zero.exchange.filter;

import com.zero.exchange.context.UserContext;
import com.zero.exchange.model.AuthToken;
import com.zero.exchange.service.CookieService;
import com.zero.exchange.support.AbstractFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UIFilterRegisterBean extends FilterRegistrationBean<Filter> {

    @Autowired
    private CookieService cookieService;

    @PostConstruct
    public void init() {
        UIFilter uiFilter = new UIFilter();
        setFilter(uiFilter);
        addUrlPatterns("/*");
        setName(uiFilter.getClass().getSimpleName());
        setOrder(100);
    }

    class UIFilter extends AbstractFilter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            if (log.isDebugEnabled()) {
                log.debug("start to do ui filter...");
            }
            // set default request encoding
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            // get token from cookie
            AuthToken auth = cookieService.findTokenInCookie(request);
            if (auth != null && auth.isAboutToExpire()) {
                log.info("refresh cookie token");
                auth.refresh();
            }
            Long userId = auth == null ? null : auth.userId();
            if (log.isDebugEnabled()) {
                log.debug("parsed userId: {} from cookie", userId);
            }
            try(UserContext context = new UserContext(userId)) {
                filterChain.doFilter(request, response);
            }
        }
    }
}
