package com.zero.exchange.support;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;

public abstract class AbstractFilter extends LoggerSupport implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("init Filter: {}...", getClass().getName());
    }

    @Override
    public void destroy() {
        log.debug("destroy Filter: {}...", getClass().getName());
    }
}
