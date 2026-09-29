package com.flexibleprojectmanager.platform.licensing.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface LicenseManagementUseCase {
    LicenseView get(CurrentActor actor);
    LicenseView activate(CurrentActor actor, String signedLicense);
    void deactivate(CurrentActor actor);
    EntitlementsView entitlements(CurrentActor actor);

    record LicenseView(UUID licenseId, UUID organizationId, UUID installationId, String status, String type,
                       Instant issuedAt, Instant expiresAt, Integer maxUsers, List<String> licenseFeatures,
                       String signatureAlgorithm) {}

    record EntitlementsView(Integer maxUsers, List<String> licenseFeatures) {}
}
