package com.flexibleprojectmanager.platform.licensing.application;

import com.flexibleprojectmanager.platform.licensing.domain.VerifiedLicense;

public interface LicenseVerifier {
    VerifiedLicense verify(String signedLicense);
}
