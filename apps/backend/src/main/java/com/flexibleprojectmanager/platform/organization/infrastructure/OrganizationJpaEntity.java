package com.flexibleprojectmanager.platform.organization.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.flexibleprojectmanager.platform.organization.domain.Organization;

@Entity
@Table(name = "organizations")
public class OrganizationJpaEntity {
    @Id @Column(name = "id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID id;
    @Column(name = "name", nullable = false, length = 200) private String name;
    @Column(name = "slug", length = 100) private String slug;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private Organization.Status status;
    @Column(name = "created_at", nullable = false) @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE) private Instant updatedAt;

    protected OrganizationJpaEntity() {}
    private OrganizationJpaEntity(Organization value) {
        id = value.id(); name = value.name(); slug = value.slug(); status = value.status();
        createdAt = value.createdAt(); updatedAt = value.updatedAt();
    }
    public static OrganizationJpaEntity from(Organization value) { return new OrganizationJpaEntity(value); }
    public Organization toDomain() { return new Organization(id, name, slug, status, createdAt, updatedAt); }
}
