package com.flexibleprojectmanager.platform.licensing.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.flexibleprojectmanager.platform.licensing.application.LicenseRepository.StoredLicense;

@Entity
@Table(name = "installation_licenses")
public class InstallationLicenseJpaEntity {
    @Id
    @Column(name = "installation_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID installationId;

    @Column(name = "signed_license", nullable = false, columnDefinition = "TEXT")
    private String signedLicense;

    @Column(name = "activated_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant activatedAt;

    @Column(name = "updated_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant updatedAt;

    protected InstallationLicenseJpaEntity() {}

    private InstallationLicenseJpaEntity(UUID installationId, String signedLicense, Instant activatedAt, Instant updatedAt) {
        this.installationId = installationId;
        this.signedLicense = signedLicense;
        this.activatedAt = activatedAt;
        this.updatedAt = updatedAt;
    }

    static InstallationLicenseJpaEntity from(StoredLicense value) {
        return new InstallationLicenseJpaEntity(value.installationId(), value.signedLicense(), value.activatedAt(), value.updatedAt());
    }

    StoredLicense toStoredLicense() {
        return new StoredLicense(installationId, signedLicense, activatedAt, updatedAt);
    }
}
