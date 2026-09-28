package com.flexibleprojectmanager.platform.installation.infrastructure;

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

import com.flexibleprojectmanager.platform.installation.domain.Installation;

@Entity @Table(name = "installations")
public class InstallationJpaEntity {
    @Id @Column(name = "id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID organizationId;
    @Column(name = "name", nullable = false, length = 200) private String name;
    @Column(name = "platform", nullable = false, length = 20) private String platform;
    @Column(name = "application_version", nullable = false, length = 50) private String applicationVersion;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private Installation.Status status;
    @Column(name = "created_at", nullable = false) @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE) private Instant createdAt;
    @Column(name = "last_seen_at") @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE) private Instant lastSeenAt;
    protected InstallationJpaEntity() {}
    private InstallationJpaEntity(Installation value) { id=value.id(); organizationId=value.organizationId(); name=value.name(); platform=value.platform(); applicationVersion=value.applicationVersion(); status=value.status(); createdAt=value.createdAt(); lastSeenAt=value.lastSeenAt(); }
    public static InstallationJpaEntity from(Installation value) { return new InstallationJpaEntity(value); }
    public Installation toDomain() { return new Installation(id, organizationId, name, platform, applicationVersion, status, createdAt, lastSeenAt); }
}
