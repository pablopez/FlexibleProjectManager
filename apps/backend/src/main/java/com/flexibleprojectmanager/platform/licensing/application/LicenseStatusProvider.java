package com.flexibleprojectmanager.platform.licensing.application;

import java.util.UUID;

import com.flexibleprojectmanager.platform.licensing.domain.EffectiveLicenseStatus;
import com.flexibleprojectmanager.platform.licensing.domain.VerifiedLicense;

public interface LicenseStatusProvider {
    LicenseEvaluation evaluate(UUID organizationId);

    record LicenseEvaluation(EffectiveLicenseStatus status, UUID organizationId, UUID installationId,
                             VerifiedLicense license) {}
}
