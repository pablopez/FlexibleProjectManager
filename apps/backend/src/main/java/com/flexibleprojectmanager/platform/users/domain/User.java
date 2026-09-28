package com.flexibleprojectmanager.platform.users.domain;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String email, String passwordHash, String displayName,
                   Status status, Instant createdAt, Instant updatedAt) {
    public enum Status { ACTIVE, DISABLED }

    public static User create(UUID id, String email, String passwordHash, String displayName, Instant now) {
        return new User(id, email, passwordHash, displayName, Status.ACTIVE, now, now);
    }

    public User update(String displayName, Status status, Instant now) {
        return new User(id, email, passwordHash,
                displayName == null ? this.displayName : displayName,
                status == null ? this.status : status, createdAt, now);
    }
}
