package com.flexibleprojectmanager.platform.users.infrastructure;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "permissions")
public class PermissionJpaEntity {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    protected PermissionJpaEntity() {}
    String code() { return code; }
}
