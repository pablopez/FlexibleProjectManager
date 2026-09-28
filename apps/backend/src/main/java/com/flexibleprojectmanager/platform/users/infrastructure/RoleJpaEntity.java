package com.flexibleprojectmanager.platform.users.infrastructure;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
public class RoleJpaEntity {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(name = "system_defined", nullable = false)
    private boolean systemDefined;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionJpaEntity> permissions = new HashSet<>();

    protected RoleJpaEntity() {}
    String code() { return code; }
    Set<PermissionJpaEntity> permissions() { return permissions; }
}
