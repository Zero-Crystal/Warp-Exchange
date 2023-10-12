package com.zero.exchange.db;

import java.util.ArrayList;
import java.util.List;

/**
 * SELECT ... FROM ... WHERE
 * */
public final class Where<T> extends CriteriaQuery<T> {

    Where(Criteria criteria, String clause, Object... params) {
        super(criteria);
        this.criteria = criteria;
        this.criteria.where = clause;
        this.criteria.whereParams = new ArrayList();
        for (Object param : params) {
            this.criteria.whereParams.add(param);
        }
    }

    /**
     * Add order by of query
     *
     * @param field
     * */
    public OrderBy<T> orderBy(String field) {
        return new OrderBy<>(this.criteria, field);
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

    /**
     * Get unique result of the query. Exception will throw if no result found or more than 1 results found.
     *
     * @return T modelInstance
     * @throws jakarta.persistence.NoResultException        If result set is empty.
     * @throws jakarta.persistence.NonUniqueResultException If more than 1 results found.
     * */
    public T unique() {
        return this.criteria.unique();
    }
}
