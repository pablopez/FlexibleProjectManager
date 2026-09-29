package com.flexibleprojectmanager.platform.licensing.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.licensing.application.LicenseRepository;

@Repository
public class JpaLicenseRepositoryAdapter implements LicenseRepository {
    private final SpringDataInstallationLicenseRepository repository;

    public JpaLicenseRepositoryAdapter(SpringDataInstallationLicenseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<StoredLicense> findByInstallationId(UUID installationId) {
        return repository.findById(installationId).map(InstallationLicenseJpaEntity::toStoredLicense);
    }

    @Override
    public StoredLicense save(UUID installationId, String signedLicense, Instant activatedAt, Instant updatedAt) {
        return repository.save(InstallationLicenseJpaEntity.from(
                new StoredLicense(installationId, signedLicense, activatedAt, updatedAt))).toStoredLicense();
    }

    @Override
    public void deleteByInstallationId(UUID installationId) {
        repository.deleteById(installationId);
    }
}
