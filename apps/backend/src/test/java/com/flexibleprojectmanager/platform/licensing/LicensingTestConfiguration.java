package com.flexibleprojectmanager.platform.licensing;

import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.flexibleprojectmanager.platform.licensing.application.LicenseStatusProvider;
import com.flexibleprojectmanager.platform.licensing.domain.EffectiveLicenseStatus;
import com.flexibleprojectmanager.platform.users.application.UserCapacityPolicy;

@TestConfiguration(proxyBeanMethods = false)
public class LicensingTestConfiguration {
    @Bean
    @Primary
    LicenseStatusProvider allowAllLicenseStatusProvider() {
        return organizationId -> new LicenseStatusProvider.LicenseEvaluation(
                EffectiveLicenseStatus.ACTIVE, organizationId, UUID.randomUUID(), null);
    }

    @Bean
    @Primary
    UserCapacityPolicy allowAllUserCapacityPolicy() {
        return organizationId -> { };
    }
}
