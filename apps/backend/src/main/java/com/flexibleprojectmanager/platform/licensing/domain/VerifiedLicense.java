package com.flexibleprojectmanager.platform.licensing.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VerifiedLicense(
        int version,
        UUID licenseId,
        UUID installationId,
        LicenseType type,
        Instant issuedAt,
        Instant expiresAt,
        int maxUsers,
        List<String> licenseFeatures,
        String signatureAlgorithm) {
    public VerifiedLicense {
        licenseFeatures = List.copyOf(licenseFeatures);
    }
}
