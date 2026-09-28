package com.flexibleprojectmanager.platform.installation.domain;

import java.time.Instant;
import java.util.UUID;

public record Installation(UUID id, UUID organizationId, String name, String platform, String applicationVersion,
                           Status status, Instant createdAt, Instant lastSeenAt) {
    public enum Status { ACTIVE, DISABLED, UNLICENSED }
    public Installation rename(String updatedName) { return new Installation(id, organizationId, updatedName, platform, applicationVersion, status, createdAt, lastSeenAt); }
}
