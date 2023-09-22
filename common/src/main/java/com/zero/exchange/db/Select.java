package com.zero.exchange.db;

import java.util.Arrays;

/**
 * SELECT ... FROM ...
 * */
public final class Select extends CriteriaQuery{

    Select(Criteria criteria, String...selectFields) {
        super(criteria);
        if (selectFields.length > 0) {
            this.criteria.select = Arrays.asList(selectFields);
        }
    }

    /**
     * Append FROM class
     * @param entityClass
     * @return From<T>
     * */
    public <T> From<T> from(Class<T> entityClass) {
        return new From<>(this.criteria, this.criteria.db.getMapper(entityClass));
    }
}
