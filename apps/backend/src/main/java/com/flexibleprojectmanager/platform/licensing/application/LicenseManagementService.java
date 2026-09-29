package com.flexibleprojectmanager.platform.licensing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.installation.application.InstallationRepository;
import com.flexibleprojectmanager.platform.installation.domain.Installation;
import com.flexibleprojectmanager.platform.licensing.application.LicenseManagementUseCase.EntitlementsView;
import com.flexibleprojectmanager.platform.licensing.application.LicenseManagementUseCase.LicenseView;
import com.flexibleprojectmanager.platform.licensing.domain.EffectiveLicenseStatus;
import com.flexibleprojectmanager.platform.licensing.domain.VerifiedLicense;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.users.application.UserCapacityPolicy;
import com.flexibleprojectmanager.platform.users.application.UserRepository;
import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;
import com.flexibleprojectmanager.platform.users.domain.User;

@Service
public class LicenseManagementService implements LicenseManagementUseCase, LicenseStatusProvider,
        LicenseFeatureProvider, UserCapacityPolicy {
    private final LicenseRepository licenses;
    private final LicenseVerifier verifier;
    private final InstallationRepository installations;
    private final UserRepository users;
    private final Clock clock;

    @Autowired
    public LicenseManagementService(LicenseRepository licenses, LicenseVerifier verifier,
                                    InstallationRepository installations, UserRepository users,
                                    Clock clock) {
        this.licenses = licenses;
        this.verifier = verifier;
        this.installations = installations;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public LicenseView get(CurrentActor actor) {
        actor.requirePermission("license:read");
        LicenseEvaluation evaluation = evaluation(actor.organizationId());
        if (evaluation.status() == EffectiveLicenseStatus.UNLICENSED) throw new LicenseNotFoundException();
        return view(evaluation);
    }

    @Override
    @Transactional
    public LicenseView activate(CurrentActor actor, String signedLicense) {
        actor.requirePermission("license:manage");
        Installation installation = installations.findCurrentByOrganizationId(actor.organizationId());
        VerifiedLicense candidate = verifier.verify(signedLicense);
        if (!candidate.installationId().equals(installation.id())) throw new LicenseException("LICENSE_INSTALLATION_MISMATCH");
        Instant now = clock.instant();
        if (candidate.issuedAt().isAfter(now)) throw new LicenseException("LICENSE_INVALID");
        if (candidate.expiresAt() != null && !now.isBefore(candidate.expiresAt())) {
            throw new LicenseException("LICENSE_EXPIRED");
        }
        if (users.countEffectiveActiveUsers(actor.organizationId()) > candidate.maxUsers()) {
            throw new LicenseException("LICENSE_USER_LIMIT_EXCEEDED");
        }
        licenses.save(installation.id(), signedLicense, now, now);
        return view(new LicenseStatusProvider.LicenseEvaluation(EffectiveLicenseStatus.ACTIVE,
                actor.organizationId(), installation.id(), candidate));
    }

    @Override
    @Transactional
    public void deactivate(CurrentActor actor) {
        actor.requirePermission("license:manage");
        Installation installation = installations.findCurrentByOrganizationId(actor.organizationId());
        if (licenses.findByInstallationId(installation.id()).isEmpty()) throw new LicenseNotFoundException();
        licenses.deleteByInstallationId(installation.id());
    }

    @Override
    @Transactional(readOnly = true)
    public EntitlementsView entitlements(CurrentActor actor) {
        actor.requirePermission("license:read");
        LicenseEvaluation evaluation = evaluation(actor.organizationId());
        if (evaluation.status() != EffectiveLicenseStatus.ACTIVE) return new EntitlementsView(null, List.of());
        return new EntitlementsView(evaluation.license().maxUsers(), evaluation.license().licenseFeatures());
    }

    @Override
    @Transactional(readOnly = true)
    public LicenseEvaluation evaluate(UUID organizationId) {
        Installation installation = installations.findCurrentByOrganizationId(organizationId);
        var stored = licenses.findByInstallationId(installation.id());
        if (stored.isEmpty()) return new LicenseEvaluation(EffectiveLicenseStatus.UNLICENSED,
                organizationId, installation.id(), null);
        try {
            VerifiedLicense license = verifier.verify(stored.get().signedLicense());
            Instant now = clock.instant();
            if (license.issuedAt().isAfter(now)) {
                return new LicenseEvaluation(EffectiveLicenseStatus.INVALID,
                        organizationId, installation.id(), null);
            }
            if (!license.installationId().equals(installation.id())) {
                return new LicenseEvaluation(EffectiveLicenseStatus.INVALID, organizationId, installation.id(), null);
            }
            EffectiveLicenseStatus status = license.expiresAt() != null
                    && !now.isBefore(license.expiresAt())
                    ? EffectiveLicenseStatus.EXPIRED : EffectiveLicenseStatus.ACTIVE;
            return new LicenseEvaluation(status, organizationId, installation.id(), license);
        } catch (RuntimeException exception) {
            return new LicenseEvaluation(EffectiveLicenseStatus.INVALID, organizationId, installation.id(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeature(UUID organizationId, String feature) {
        if (feature == null || feature.isBlank()) return false;
        LicenseEvaluation evaluation = evaluate(organizationId);
        return evaluation.status() == EffectiveLicenseStatus.ACTIVE
                && evaluation.license().licenseFeatures().contains(feature);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCapacityForAdditionalActiveUser(UUID organizationId) {
        LicenseEvaluation evaluation = evaluate(organizationId);
        if (evaluation.status() != EffectiveLicenseStatus.ACTIVE) throw new LicenseNotActiveException();
        if (users.countEffectiveActiveUsers(organizationId) >= evaluation.license().maxUsers()) {
            throw new LicenseException("LICENSE_USER_LIMIT_EXCEEDED");
        }
    }

    private LicenseEvaluation evaluation(UUID organizationId) {
        return evaluate(organizationId);
    }

    private LicenseView view(LicenseEvaluation evaluation) {
        VerifiedLicense license = evaluation.license();
        return new LicenseView(
                license == null ? null : license.licenseId(),
                evaluation.organizationId(), evaluation.installationId(), evaluation.status().name(),
                license == null ? null : license.type().name(),
                license == null ? null : license.issuedAt(),
                license == null ? null : license.expiresAt(),
                license == null ? null : license.maxUsers(),
                license == null ? null : license.licenseFeatures(),
                license == null ? null : license.signatureAlgorithm());
    }
}
