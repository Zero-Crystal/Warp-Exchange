package com.zero.exchange.db;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Represent a bean public field with JPA annotation.
 */
public class AccessibleProperty {

    static final Map<Class<?>, String> DEFAULT_COLUMN_TYPES = new HashMap<>();

    static  {
        DEFAULT_COLUMN_TYPES.put(String.class, "VARCHAR($1)");

        DEFAULT_COLUMN_TYPES.put(boolean.class, "BIT");
        DEFAULT_COLUMN_TYPES.put(Boolean.class, "BIT");

        DEFAULT_COLUMN_TYPES.put(byte.class, "TINYINT");
        DEFAULT_COLUMN_TYPES.put(Byte.class, "TINYINT");

        DEFAULT_COLUMN_TYPES.put(int.class, "INTEGER");
        DEFAULT_COLUMN_TYPES.put(Integer.class, "INTEGER");

        DEFAULT_COLUMN_TYPES.put(short.class, "SMALLINT");
        DEFAULT_COLUMN_TYPES.put(Short.class, "SMALLINT");

        DEFAULT_COLUMN_TYPES.put(long.class, "BIGINT");
        DEFAULT_COLUMN_TYPES.put(Long.class, "BIGINT");

        DEFAULT_COLUMN_TYPES.put(float.class, "REAL");
        DEFAULT_COLUMN_TYPES.put(Float.class, "REAL");

        DEFAULT_COLUMN_TYPES.put(double.class, "DOUBLE");
        DEFAULT_COLUMN_TYPES.put(Double.class, "DOUBLE");

        DEFAULT_COLUMN_TYPES.put(BigDecimal.class, "DECIMAL($1,$2)");
    }

    final Field field;

    final Class<?> propertyType;

    final String propertyName;

    final String columnDefinition;

    final Function<Object, Object> javaToSqlMapper;

    final Function<Object, Object> sqlMapperToJava;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AccessibleProperty(Field field) {
        this.field = field;
        this.propertyType = this.field.getType();
        this.propertyName = this.field.getName();
        this.columnDefinition = getColumnDefinition(propertyType);
        boolean isEnum = this.propertyType.isEnum();
        this.javaToSqlMapper = isEnum ? (obj) -> ((Enum<?>) obj).name() : null;
        this.sqlMapperToJava = isEnum ? (obj) -> Enum.valueOf((Class<? extends Enum>) this.propertyType, (String) obj)
                : null;
    }

    public Object get(Object bean) throws ReflectiveOperationException {
        Object value = this.field.get(bean);
        if (this.javaToSqlMapper != null) {
            value = this.javaToSqlMapper.apply(value);
        }
        return value;
    }

    public void set(Object bean, Object value) throws ReflectiveOperationException {
        if (this.sqlMapperToJava != null) {
            value = this.sqlMapperToJava.apply(value);
        }
        this.field.set(bean, value);
    }

    /**
     * 是否是ID
     * */
    boolean isId() {
        return this.field.getAnnotation(Id.class) != null;
    }

    /**
     * is id && is id marked as @GeneratedValue(strategy=GenerationType.IDENTITY)
     * */
    boolean isIdentityId() {
        if (!isId()) {
            return false;
        }
        // @GeneratedValue：用来标识为实体生成一个唯一标识的主键；strategy ———— 生成策略
        GeneratedValue gv = this.field.getAnnotation(GeneratedValue.class);
        if (gv == null) {
            return false;
        }
        GenerationType gt = gv.strategy();
        return gt == GenerationType.IDENTITY;
    }

    boolean isInsert() {
        if (isIdentityId()) {
            return false;
        }
        Column col = this.field.getAnnotation(Column.class);
        return col == null || col.insertable();
    }

    boolean isUpdate() {
        if (isId()) {
            return false;
        }
        Column col = this.field.getAnnotation(Column.class);
        return col == null || col.updatable();
    }

    private String getColumnDefinition(Class<?> propertyType) {
        Column column = this.field.getAnnotation(Column.class);
        if (column == null) {
            throw new IllegalArgumentException("@Column not found in filed: " + this.field);
        }
        String colDefinition = null;
        if (column != null || column.columnDefinition().isEmpty()) {
            if (propertyType.isEnum()) {
                colDefinition = "VARCHAR(32)";
            } else {
                colDefinition = getDefaultColumnType(propertyType, column);
            }
        } else {
            colDefinition = column.columnDefinition().toUpperCase(Locale.ROOT);
        }
        // 是否可空
        boolean colNullAble = column == null ? true : column.nullable();
        colDefinition = colDefinition + " " + (colNullAble ? "NULL" : "NOT NULL");
        // 是否存在主键自增
        if (isIdentityId()) {
            colDefinition = colDefinition + " AUTO_INCREMENT";
        }
        // 是否存在唯一键
        if (!isId() && column != null  && column.unique()) {
            colDefinition = colDefinition + " UNIQUE";
        }
        return colDefinition;
    }

    private static String getDefaultColumnType(Class<?> type, Column column) {
        String colType = DEFAULT_COLUMN_TYPES.get(type);
        if (colType.equals("VARCHAR($1)")) {
            colType = colType.replace("$1", String.valueOf(column == null ? 255 : column.length()));
        }
        if (colType.equals("DECIMAL($1,$2)")) {
            int precision = column == null ? 100 : column.precision();
            int scale = column == null ? 2 : column.scale();
            colType = colType.replace("$1", String.valueOf(precision)).replace("$2", String.valueOf(scale));
        }
        return colType;
    }

    @Override
    public String toString() {
        return "AccessibleProperty [" + "field: " + field + ", propertyType: " + propertyType +
                ", propertyName: " + propertyName + ", columnDefinition: " + columnDefinition +
                ", javaToSqlMapper: " + javaToSqlMapper + ", sqlMapperToJava: " + sqlMapperToJava + ']';
    }
}
