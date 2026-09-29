package com.flexibleprojectmanager.platform.licensing.application;

import java.util.UUID;

public interface LicenseFeatureProvider {
    boolean hasFeature(UUID organizationId, String feature);
}
