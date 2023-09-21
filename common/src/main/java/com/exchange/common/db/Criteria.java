package com.exchange.common.db;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import java.util.ArrayList;
import java.util.List;

final class Criteria<T> {

    DbTemplate db;
    Mapper<T> mapper;
    Class<T> clazz;
    List<String> select = null;
    String table = null;
    boolean distinct = false;
    String where = null;
    List<Object> whereParams = null;
    List<String> orderBy = null;
    int offset = 0;
    int maxResults = 0;

    Criteria(DbTemplate db) {
        this.db = db;
    }

    String sql() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");
        builder.append(select == null ? "*" : String.join(", ", select));
        builder.append(" FROM ").append(table);
        if (where != null) {
            builder.append("WHERE ").append(String.join(" ", where));
        }
        if (orderBy != null) {
            builder.append("ORDER BY ").append(String.join(", ", orderBy));
        }
        if (offset >= 0 && maxResults > 0) {
            builder.append("LIMIT ?, ?");
        }
        return builder.toString();
    }

    Object[] params() {
        List<Object> params = new ArrayList<>();
        for (Object obj : whereParams) {
            if (obj == null) {
                params.add(null);
            } else {
                params.add(obj);
            }
        }
        if (offset >= 0 && maxResults > 0) {
            params.add(offset);
            params.add(maxResults);
        }
        return params.toArray();
    }

    List<T> list() {
        String sql = sql();
        Object[] params = params();
        return db.jdbcTemplate.query(sql, mapper.resultSet, params);
    }

    T first() {
        this.offset = 0;
        this.maxResults = 1;
        String sql = sql();
        Object[] params = params();
        List<T> results = db.jdbcTemplate.query(sql, mapper.resultSet, params);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    T unique() {
        this.offset = 0;
        this.maxResults = 2;
        String sql = sql();
        Object[] params = params();
        List<T> results = db.jdbcTemplate.query(sql, mapper.resultSet, params);
        if (results.isEmpty()) {
            throw new NoResultException("except unique row but nothing found");
        }
        if (results.size() > 1) {
            throw new NonUniqueResultException("except unique row but found more than 1 result");
        }
        return results.get(0);
    }
}
