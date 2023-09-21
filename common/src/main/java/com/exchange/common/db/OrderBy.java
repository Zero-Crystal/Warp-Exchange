package com.exchange.common.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SELECT ... FROM ... ORDER BY ...
 * */
public final class OrderBy<T> extends CriteriaQuery<T>{

    OrderBy(Criteria<T> criteria, String orderBy) {
        super(criteria);
        this.criteria = criteria;
    }

    /**
     * Add order by of query
     *
     * @param orderBy
     * @return OrderBy<T>
     * */
    public OrderBy<T> orderBy(String orderBy) {
        if (this.criteria.orderBy == null) {
            this.criteria.orderBy = new ArrayList<>();
        }
        this.criteria.orderBy.add(orderBy);
        return this;
    }

    /**
     * Add desc
     *
     * @return OrderBy<T>
     * */
    public OrderBy<T> desc() {
        int lastIndex = this.criteria.orderBy.size() - 1;
        String last =  this.criteria.orderBy.get(lastIndex);
        if (!last.toUpperCase(Locale.ROOT).endsWith(" DESC")) {
            last += " DESC";
        }
        this.criteria.orderBy.set(lastIndex, last);
        return this;
    }

    /**
     * Add limit of query
     *
     * @param maxResult
     * @return Limit<T>
     * */
    public Limit<T> limit(int maxResult) {
        return limit(0, maxResult);
    }

    /**
     * Add limit of query
     *
     * @param offset
     * @param maxResults
     * @return Limit<T>
     * */
    public Limit<T> limit(int offset, int maxResults) {
        return new Limit<>(this.criteria, offset, maxResults);
    }

    /**
     * get all results
     * */
    public List<T> list() {
        return this.criteria.list();
    }

    /**
     * get first raw of result, return null if not found
     * */
    public T first() {
        return this.criteria.first();
    }
}
