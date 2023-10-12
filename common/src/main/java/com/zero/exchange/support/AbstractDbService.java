package com.zero.exchange.support;

import com.zero.exchange.db.DbTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDbService extends LoggerSupport {

    @Autowired
    protected DbTemplate db;
}
