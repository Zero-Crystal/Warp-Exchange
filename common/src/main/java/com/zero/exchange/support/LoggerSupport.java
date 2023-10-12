package com.zero.exchange.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoggerSupport {

    /**
     * 子类可以直接使用logger
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

}
