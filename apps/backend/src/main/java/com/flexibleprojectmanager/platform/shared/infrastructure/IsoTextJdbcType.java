package com.flexibleprojectmanager.platform.shared.infrastructure;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.time.Instant;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/** Binds UUID and Instant values as their ISO text representation for SQLite. */
public class IsoTextJdbcType extends VarcharJdbcType {
    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new ValueBinder<>() {
            @Override
            public void bind(PreparedStatement statement, X value, int index, WrapperOptions options) throws SQLException {
                statement.setString(index, value == null ? null : value.toString());
            }

            @Override
            public void bind(CallableStatement statement, X value, String name, WrapperOptions options) throws SQLException {
                statement.setString(name, value == null ? null : value.toString());
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new ValueExtractor<>() {
            @Override
            public X extract(ResultSet resultSet, int index, WrapperOptions options) throws SQLException {
                return convert(resultSet.getString(index), javaType);
            }

            @Override
            public X extract(java.sql.CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                return convert(statement.getString(index), javaType);
            }

            @Override
            public X extract(java.sql.CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                return convert(statement.getString(name), javaType);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <X> X convert(String value, JavaType<X> javaType) {
        if (value == null) return null;
        Class<?> type = javaType.getJavaTypeClass();
        if (type == UUID.class) return (X) UUID.fromString(value);
        if (type == Instant.class) return (X) Instant.parse(value);
        return javaType.wrap(value, null);
    }
}
