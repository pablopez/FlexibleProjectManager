package com.flexibleprojectmanager.platform.licensing.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository {
    Optional<StoredLicense> findByInstallationId(UUID installationId);
    StoredLicense save(UUID installationId, String signedLicense, Instant activatedAt, Instant updatedAt);
    void deleteByInstallationId(UUID installationId);

    record StoredLicense(UUID installationId, String signedLicense, Instant activatedAt, Instant updatedAt) {}
}
