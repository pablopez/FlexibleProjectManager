package com.flexibleprojectmanager.platform.audit.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;

@Entity
@Table(name = "audit_events")
public class AuditEventJpaEntity {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false)
    private UUID id;
    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "actor_user_id")
    private UUID actorUserId;
    @Column(nullable = false, length = 64) private String action;
    @Column(name = "resource_type", nullable = false, length = 64) private String resourceType;
    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "resource_id") private UUID resourceId;
    @Column(columnDefinition = "TEXT") private String metadata;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE) @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AuditEventJpaEntity() {}

    private AuditEventJpaEntity(AuditEvent event, String metadata) {
        id = event.id(); organizationId = event.organizationId(); actorUserId = event.actorUserId(); action = event.action();
        resourceType = event.resourceType(); resourceId = event.resourceId(); this.metadata = metadata; createdAt = event.occurredAt();
    }

    static AuditEventJpaEntity from(AuditEvent event, String metadata) { return new AuditEventJpaEntity(event, metadata); }
    UUID id() { return id; }
    UUID organizationId() { return organizationId; }
    UUID actorUserId() { return actorUserId; }
    String action() { return action; }
    String resourceType() { return resourceType; }
    UUID resourceId() { return resourceId; }
    String metadata() { return metadata; }
    Instant createdAt() { return createdAt; }
}
