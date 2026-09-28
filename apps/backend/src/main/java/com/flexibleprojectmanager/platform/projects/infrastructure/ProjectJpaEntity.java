package com.flexibleprojectmanager.platform.projects.infrastructure;

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

import com.flexibleprojectmanager.platform.projects.domain.Project;

@Entity
@Table(name = "projects")
public class ProjectJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID organizationId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Project.Status status;

    @Column(name = "created_by", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant updatedAt;

    protected ProjectJpaEntity() {
    }

    private ProjectJpaEntity(Project project) {
        this.id = project.id();
        this.organizationId = project.organizationId();
        this.name = project.name();
        this.description = project.description();
        this.status = project.status();
        this.createdBy = project.createdBy();
        this.createdAt = project.createdAt();
        this.updatedAt = project.updatedAt();
    }

    public static ProjectJpaEntity from(Project project) {
        return new ProjectJpaEntity(project);
    }

    public Project toDomain() {
        return new Project(id, organizationId, name, description, status, createdBy, createdAt, updatedAt);
    }
}
