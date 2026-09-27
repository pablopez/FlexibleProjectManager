package com.flexibleprojectmanager.platform.authentication.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationRepository {
    Optional<AuthenticationIdentity> findByEmail(String email);

    Optional<AuthenticationIdentity> findByUserId(UUID userId);

    void saveRefreshSession(RefreshSession session);

    Optional<RefreshSession> findRefreshSessionByHash(String tokenHash);

    boolean rotateRefreshSession(UUID currentId, UUID replacementId, Instant revokedAt);

    void revokeRefreshSession(String tokenHash, Instant revokedAt);
}
