package com.flexibleprojectmanager.platform.authentication.application;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        UUID replacedByTokenId) {

    public boolean usableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
