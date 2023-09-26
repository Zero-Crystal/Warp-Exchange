package com.zero.exchange.db;

import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

final class Mapper<T> {

    static List<String> columnDefinitionSortBy = Arrays.asList("BIT", "BOOL", "TINYINT", "SMALLINT", "MEDIUMINT", "INT",
            "INTEGER", "BIGINT", "FLOAT", "REAL", "DOUBLE", "DECIMAL", "YEAR", "DATE", "TIME", "DATETIME", "TIMESTAMP",
            "VARCHAR", "CHAR", "BLOB", "TEXT", "MEDIUMTEXT");

    static int columnDefinitionSortIndex(String definition) {
        int pos = definition.indexOf("(");
        if (pos > 0) {
            definition = definition.substring(0, pos).toUpperCase(Locale.ROOT);
        }
        int index = columnDefinitionSortBy.indexOf(definition);
        return index == -1 ? Integer.MAX_VALUE : index;
    }

    final Class<T> entityClass;

    final Constructor<T> constructor;

    final String tableName;

    // Id
    final AccessibleProperty id;

    // all properties
    final List<AccessibleProperty> allProperties;

    final Map<String, AccessibleProperty> allPropertyMap;

    // update property
    final List<AccessibleProperty> updateProperties;

    //property name -> property
    final Map<String, AccessibleProperty> updatePropertyMap;

    //insert property
    final List<AccessibleProperty> insertProperties;

    // result
    final ResultSetExtractor<List<T>> resultSet;

    final String selectSQL;

    final String insertSQL;

    final String insertIgnoreSQL;

    final String updateSQL;

    final String deleteSQL;

    public T newInstance() throws ReflectiveOperationException {
        return this.constructor.newInstance();
    }

    public Mapper(Class<T> clazz) throws ReflectiveOperationException {
        this.entityClass = clazz;
        this.constructor = clazz.getConstructor();
        this.tableName = getTableName(this.entityClass);
        List<AccessibleProperty> allProperty = getAllProperties(this.entityClass);
        AccessibleProperty[] ids = allProperty.stream().filter(AccessibleProperty::isId).toArray(AccessibleProperty[]::new);
        this.id = ids[0];
        this.allProperties = allProperty;
        this.allPropertyMap = buildPropertyMap(allProperty);
        this.updateProperties = this.allProperties.stream().filter(AccessibleProperty::isUpdate).collect(Collectors.toList());
        this.updatePropertyMap = buildPropertyMap(this.updateProperties);
        this.insertProperties = this.allProperties.stream().filter(AccessibleProperty::isInsert).collect(Collectors.toList());
        this.selectSQL = "SELECT * FROM " + this.tableName + " WHERE " + this.id.propertyName + " = ?";
        this.insertSQL = "INSERT INTO " + this.tableName + "(" + String.join(", ", this.insertProperties.stream().map(p -> p.propertyName).toArray(String[]::new))
                + ") VALUES (" + numberOfQuestions(this.insertProperties.size()) + ")";
        this.insertIgnoreSQL = this.insertSQL.replace("INSERT INTO", "INSERT IGNORE INTO");
        this.updateSQL = "UPDATE " + this.tableName + " SET " + String.join(", ",
                this.updateProperties.stream().map(p -> p.propertyName + " = ?").toArray(String[]::new))
                + " WHERE " + this.id.propertyName + " = ?";
        this.deleteSQL = "DELETE FROM " + this.tableName + " WHERE " + this.id.propertyName + " = ?";
        this.resultSet = new ResultSetExtractor<>() {
            @Override
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<T> resultList = new ArrayList<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnNum = metaData.getColumnCount();
                String[] names = new String[columnNum];
                for (int i = 0; i < columnNum; i++) {
                    names[i] = metaData.getColumnLabel(i + 1);
                }
                while (rs.next()) {
                    try {
                        T bean = newInstance();
                        for (int i = 0; i < columnNum; i++) {
                            AccessibleProperty property = allPropertyMap.get(names[i]);
                            if (property != null) {
                                property.set(bean, rs.getObject(i + 1));
                            }
                        }
                        resultList.add(bean);
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                }
                return resultList;
            }
        };
    }

    public String ddl() {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(this.tableName).append(" (\n");
        //columns
        builder.append(String.join(",\n", this.allProperties.stream().sorted((o1, o2) -> {
            // sort by id
            if (o1.isId()) {
                return -1;
            }
            if (o2.isId()) {
                return 1;
            }
            // sort by columnName:
            return o1.propertyName.compareTo(o2.propertyName);
        }).map(p -> {
            return " " + p.propertyName + " " + p.columnDefinition;
        }).toArray(String[]::new)));
        builder.append(",\n");
        // unique key
        builder.append(getUniqueKey());
        // index key
        builder.append(getIndex());
        // primary key
        builder.append("PRIMARY KEY(").append(this.id.propertyName).append(")\n");
        builder.append(") CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;\n");
        return builder.toString();
    }

    String getUniqueKey() {
        Table table = this.entityClass.getAnnotation(Table.class);
        if (table != null) {
            return Arrays.stream(table.uniqueConstraints()).map(c -> {
                String name = c.name().isEmpty() ? "UNI_" + String.join("_", c.columnNames()) : c.name();
                return "CONSTRAINT " + name + " UNIQUE (" + String.join(", ", c.columnNames()) + "), \n";
            }).reduce("", (acc, s) -> {
                return acc + s;
            });
        }
        return "";
    }

    String getIndex() {
        Table table = this.entityClass.getAnnotation(Table.class);
        if (table != null) {
            return Arrays.stream(table.indexes()).map(c -> {
                if (c.unique()) {
                    String name = c.name().isEmpty() ? "UNI_" + c.columnList().replace(" ", "").replace(",", "_")
                            : c.name();
                    return "CONSTRAINT " + name + " (" + c.columnList() + "), \n";
                } else {
                    String name = c.name().isEmpty() ? "IDX_" +  c.columnList().replace(" ", "").replace(",", "_")
                            : c.name();
                    return "INDEX " + name + " (" + c.columnList() + "), \n";
                }
            }).reduce("", (acc, s) -> {
                return acc + s;
            });
        }
        return "";
    }

    Object getIdValue(Object bean) throws ReflectiveOperationException {
        return this.id.get(bean);
    }

    private String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null) {
            return table.name();
        }
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private List<AccessibleProperty> getAllProperties(Class<?> clazz) {
        List<AccessibleProperty> accessibleProperties = new ArrayList<>();
        for (Field field : clazz.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            // @Transient: 添加表中不存在的字段
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            var properTy = new AccessibleProperty(field);
            accessibleProperties.add(properTy);
        }
        return accessibleProperties;
    }

    private Map<String, AccessibleProperty> buildPropertyMap(List<AccessibleProperty> propertyList) {
        Map<String, AccessibleProperty> propertyMap = new HashMap<>();
        for (AccessibleProperty accessibleProperty : propertyList) {
            propertyMap.put(accessibleProperty.propertyName, accessibleProperty);
        }
        return propertyMap;
    }

    private String numberOfQuestions(int n) {
        String[] qs = new String[n];
        return String.join(", ", Arrays.stream(qs).map((s) -> {
            return "?";
        }).toArray(String[]::new));
    }
}
