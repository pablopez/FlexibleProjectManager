package com.flexibleprojectmanager.platform.licensing.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInstallationLicenseRepository extends JpaRepository<InstallationLicenseJpaEntity, UUID> {
}
