package com.exchange.common.db;

import java.util.List;

/**
 * SELECT ... FROM ... LIMIT ...
 * */
public final class Limit<T> extends CriteriaQuery<T> {

    Limit(Criteria<T> criteria, int offset, int maxResults) {
        super(criteria);
        if (offset < 0) {
            throw new IllegalArgumentException("offset must >= 0");
        }
        if (maxResults < 0) {
            throw new IllegalArgumentException("max result must >= 0");
        }
        this.criteria.offset = offset;
        this.criteria.maxResults = maxResults;
    }

    /**
     * get all result
     * */
    public List<T> list() {
        return this.criteria.list();
    }
}
