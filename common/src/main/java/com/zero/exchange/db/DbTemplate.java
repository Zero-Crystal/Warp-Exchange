package com.zero.exchange.db;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Stream;

@Component
public class DbTemplate {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final JdbcTemplate jdbcTemplate;

    private Map<Class<?>, Mapper<?>> classMapping;

    public DbTemplate(@Autowired JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        String currentPackage = getClass().getPackageName();
        int pos = currentPackage.lastIndexOf(".");
        String entityPackage = currentPackage.substring(0, pos) + ".model";
        List<Class<?>> entityClassList = scanEntities(entityPackage);
        Map<Class<?>, Mapper<?>> entityClassMapper = new HashMap<>();
        try {
            for (Class<?> clazz : entityClassList) {
                Mapper<?> mapper = new Mapper<>(clazz);
                entityClassMapper.put(clazz, mapper);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.classMapping = entityClassMapper;
    }

    /**
     * get Table name
     *
     * @param clazz entity class
     * */
    public String getTable(Class<?> clazz) {
        return this.classMapping.get(clazz).tableName;
    }

    /**
     * get entity by class and id
     *
     * @param clazz
     * @param id
     * @throws EntityExistsException
     * */
    public <T> T get(Class<T> clazz, Object id) {
        T entity = fetch(clazz, id);
        if (entity == null) {
            throw new EntityExistsException("entity[" + clazz.getName() + "] not found");
        }
        return entity;
    }

    /**
     * fetch entity by class and id
     * return null if not found
     *
     * @param clazz
     * @param id
     * */
    public <T> T fetch(Class<T> clazz, Object id) {
        Mapper<T> mapper = getMapper(clazz);
        List<T> list = this.jdbcTemplate.query(mapper.selectSQL, mapper.resultSet, id);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public String exportDDL() {
        return String.join("\n\n", this.classMapping.values().stream().map((mapper) -> {
            return mapper.ddl();
        }).sorted().toArray(String[]::new));
    }

    /**
     * remove bean by id
     *
     * @param bean
     * */
    public <T> void delete(Class<T> bean) {
        try {
            Mapper<T> mapper = getMapper(bean);
            delete(bean, mapper.getIdValue(bean));
        } catch (ReflectiveOperationException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * remove bean by id
     *
     * @param clazz
     * @param id
     * */
    public <T> void delete(Class<T> clazz, Object id) {
        Mapper<T> mapper = getMapper(clazz);
        if (logger.isDebugEnabled()) {
            logger.debug("SQL: ------> {}", mapper.deleteSQL);
        }
        jdbcTemplate.update(mapper.deleteSQL, id);
    }

    /**
     * select bean
     *
     * @param fields
     * @return Select
     * */
    public Select select(String... fields) {
        return new Select(new Criteria<>(this), fields);
    }

    /**
     * select bean from entity
     *
     * @param entityClass
     * @return <T> From<T>
     * */
    public <T> From<T> from(Class<T> entityClass) {
        Mapper<T> mapper = getMapper(entityClass);
        return new From<>(new Criteria<>(this), mapper);
    }

    /**
     * update bean
     *
     * @param bean
     * */
    public <T> void update(T bean) {
        try {
            Mapper<?> mapper = getMapper(bean.getClass());
            Object[] updateParams = new Object[mapper.updateProperties.size() + 1];
            int n = 0;
            for (AccessibleProperty property : mapper.updateProperties) {
                updateParams[n] = property.get(bean);
                n++;
            }
            updateParams[n] = mapper.id.get(bean);
            if (logger.isDebugEnabled()) {
                logger.debug("SQL: {}", mapper.updateSQL);
            }
            jdbcTemplate.update(mapper.updateSQL, updateParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * insert one bean
     * */
    public <T> void insert(T bean) {
        doInsert(bean, false);
    }

    /**
     * insert one bean with ignore
     * */
    public <T> void insertIgnore(T bean) {
        doInsert(bean, true);
    }

    /**
     * insert bean list
     * */
    public <T> void inset(List<T> beans) {
        for (T bean : beans) {
            doInsert(bean, false);
        }
    }

    /**
     * insert bean list with ignore
     * */
    public <T> void insetIgnore(List<T> beans) {
        for (T bean : beans) {
            doInsert(bean, true);
        }
    }

    /**
     * insert bean list
     * */
    public <T> void inset(Stream<T> beans) {
        beans.forEach(b -> {
            doInsert(b, false);
        });
    }

    /**
     * insert bean list with ignore
     * */
    public <T> void insetIgnore(Stream<T> beans) {
        beans.forEach(b -> {
            doInsert(b, true);
        });
    }

    /**
     * insert bean
     *
     * @param bean
     * @param isIgnore
     * */
    private <T> void doInsert(T bean, boolean isIgnore) {
        try {
            Mapper<?> mapper = getMapper(bean.getClass());
            Object[] insertParams = new Object[mapper.insertProperties.size()];
            int n = 0;
            for (AccessibleProperty property : mapper.insertProperties) {
                insertParams[n] = property.get(bean);
                n++;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("SQL: {}", isIgnore ? mapper.insertIgnoreSQL : mapper.insertSQL);
            }
            int rows;
            if (mapper.id.isIdentityId()) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                rows = jdbcTemplate.update(new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        PreparedStatement ps = con.prepareStatement(isIgnore ? mapper.insertIgnoreSQL : mapper.insertSQL,
                                Statement.RETURN_GENERATED_KEYS);
                        for (int i = 0; i < insertParams.length; i++) {
                            ps.setObject(i + 1, insertParams[i]);
                        }
                        return ps;
                    }
                }, keyHolder);
                // 更新自增主键
                if (rows == 1) {
                    Number key = keyHolder.getKey();
                    if (key instanceof BigInteger) {
                        key = ((BigInteger) key).longValueExact();
                    }
                    mapper.id.set(bean, key);
                }
            } else {
                rows = jdbcTemplate.update(isIgnore ? mapper.insertIgnoreSQL : mapper.insertSQL, insertParams);
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    <T> Mapper<T> getMapper(Class<T> clazz) {
        Mapper<T> mapper = (Mapper<T>) this.classMapping.get(clazz);
        if (mapper == null) {
            throw new RuntimeException("class[" + clazz.getName() + "] not exist");
        }
        return mapper;
    }

    private List<Class<?>> scanEntities(String classPackage) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        Set<BeanDefinition> beanDefinitionSet = provider.findCandidateComponents(classPackage);
        List<Class<?>> scanClassList = new ArrayList<>();
        for (BeanDefinition bean : beanDefinitionSet) {
            try {
                scanClassList.add(Class.forName(bean.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return scanClassList;
    }
}
