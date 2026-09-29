package com.flexibleprojectmanager.platform.licensing.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LicenseDtos {
    private LicenseDtos() {}

    public record LicenseResponse(UUID licenseId, UUID organizationId, UUID installationId, String status,
                                  String type, Instant issuedAt, Instant expiresAt, List<String> licenseFeatures,
                                  LicenseLimits limits, String signatureAlgorithm) {}

    public record LicenseLimits(Integer maxUsers) {}

    public record ActivateLicenseRequest(String signedLicense) {}

    public record EntitlementsResponse(Integer maxUsers, List<String> licenseFeatures) {}
}
