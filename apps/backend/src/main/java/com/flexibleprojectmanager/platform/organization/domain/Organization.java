package com.flexibleprojectmanager.platform.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record Organization(UUID id, String name, String slug, Status status, Instant createdAt, Instant updatedAt) {
    public enum Status { ACTIVE, DISABLED }

    public Organization update(String updatedName, String updatedSlug, Instant now) {
        return new Organization(id, updatedName, updatedSlug, status, createdAt, now);
    }
}
