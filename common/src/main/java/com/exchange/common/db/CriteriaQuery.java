package com.exchange.common.db;

public abstract class CriteriaQuery<T> {

    protected Criteria<T> criteria;

    CriteriaQuery(Criteria<T> criteria) {
        this.criteria = criteria;
    }

    String sql() {
        return criteria.sql();
    }
}
