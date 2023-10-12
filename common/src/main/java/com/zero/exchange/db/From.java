package com.zero.exchange.db;

import java.util.List;

/**
 * SELECT ... FROM ...
 * */
public final class From<T> extends CriteriaQuery<T> {

    From(Criteria<T> criteria, Mapper<T> mapper) {
        super(criteria);
        this.criteria.mapper = mapper;
        this.criteria.clazz = mapper.entityClass;
        this.criteria.table = mapper.tableName;
    }

    /**
     * Append where class
     * @param clause String
     * @param params Object...
     * @return Where<T>
     * */
    public Where<T> where(String clause, Object... params) {
        return new Where<>(this.criteria, clause, params);
    }

    /**
     * Add order by of query
     *
     * @param orderBy
     * */
    public OrderBy<T> orderBy(String orderBy) {
        return new OrderBy<>(this.criteria, orderBy);
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
