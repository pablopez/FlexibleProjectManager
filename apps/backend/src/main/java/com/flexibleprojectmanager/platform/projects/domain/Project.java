package com.flexibleprojectmanager.platform.projects.domain;

import java.time.Instant;
import java.util.UUID;

/** The generic Platform Core project aggregate. Domain modules compose around its id. */
public record Project(
        UUID id,
        UUID organizationId,
        String name,
        String description,
        Status status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public enum Status { ACTIVE, ARCHIVED }

    public static Project create(UUID organizationId, UUID createdBy, String name, String description, Instant now) {
        if (organizationId == null || createdBy == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project organization, creator and name are required.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 200) throw new IllegalArgumentException("Project name is too long.");
        if (description != null && description.length() > 2000) throw new IllegalArgumentException("Project description is too long.");
        return new Project(UUID.randomUUID(), organizationId, normalizedName, description, Status.ACTIVE, createdBy, now, now);
    }

    public Project update(String newName, boolean nameSupplied, String newDescription, boolean descriptionSupplied, Instant now) {
        String updatedName = !nameSupplied ? name : newName.trim();
        if (updatedName.isBlank() || updatedName.length() > 200) throw new IllegalArgumentException("Project name is invalid.");
        if (descriptionSupplied && newDescription != null && newDescription.length() > 2000) throw new IllegalArgumentException("Project description is too long.");
        return new Project(id, organizationId, updatedName, descriptionSupplied ? newDescription : description, status, createdBy, createdAt, now);
    }

    public Project archive(Instant now) {
        if (status == Status.ARCHIVED) throw new IllegalStateException("Project is already archived.");
        return new Project(id, organizationId, name, description, Status.ARCHIVED, createdBy, createdAt, now);
    }

    public Project restore(Instant now) {
        if (status == Status.ACTIVE) throw new IllegalStateException("Project is already active.");
        return new Project(id, organizationId, name, description, Status.ACTIVE, createdBy, createdAt, now);
    }
}
