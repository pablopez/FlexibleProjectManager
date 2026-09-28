package com.flexibleprojectmanager.platform.shared.infrastructure;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;

/**
 * SQLite stores the UUID and Instant columns as ISO text. PostgreSQL keeps its
 * native UUID and TIMESTAMPTZ mappings through its standard dialect.
 */
public class PlatformSQLiteDialect extends SQLiteDialect {
    @Override
    protected String columnType(int sqlTypeCode) {
        if (sqlTypeCode == SqlTypes.UUID || sqlTypeCode == SqlTypes.TIMESTAMP_WITH_TIMEZONE) {
            return "text";
        }
        return super.columnType(sqlTypeCode);
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        var registry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        registry.addDescriptor(SqlTypes.UUID, new IsoTextJdbcType());
        registry.addDescriptor(SqlTypes.TIMESTAMP_WITH_TIMEZONE, new IsoTextJdbcType());
    }
}
