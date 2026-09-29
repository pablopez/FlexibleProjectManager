package com.flexibleprojectmanager.platform.licensing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.installation.application.InstallationRepository;
import com.flexibleprojectmanager.platform.installation.domain.Installation;
import com.flexibleprojectmanager.platform.licensing.application.LicenseException;
import com.flexibleprojectmanager.platform.licensing.application.LicenseManagementService;
import com.flexibleprojectmanager.platform.licensing.application.LicenseNotActiveException;
import com.flexibleprojectmanager.platform.licensing.application.LicenseRepository;
import com.flexibleprojectmanager.platform.licensing.application.LicenseVerifier;
import com.flexibleprojectmanager.platform.licensing.domain.EffectiveLicenseStatus;
import com.flexibleprojectmanager.platform.licensing.domain.LicenseType;
import com.flexibleprojectmanager.platform.licensing.domain.VerifiedLicense;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.users.application.RoleView;
import com.flexibleprojectmanager.platform.users.application.UserRepository;
import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;
import com.flexibleprojectmanager.platform.users.domain.User;

class LicenseManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID INSTALLATION_ID = UUID.randomUUID();
    private static final CurrentActor ACTOR = new CurrentActor(
            UUID.randomUUID(), ORGANIZATION_ID, UUID.randomUUID(), List.of("license:read", "license:manage"));

    @Test
    void evaluatesExpiryBoundariesAndFutureIssuedArtifacts() {
        VerifiedLicense active = license(NOW.minusSeconds(10), NOW.plusSeconds(10), 10, List.of("module.video-qc"));
        FakeLicenseRepository repository = new FakeLicenseRepository();
        repository.stored = stored("active");
        LicenseManagementService service = service(repository, value -> active, 0);

        assertEquals(EffectiveLicenseStatus.ACTIVE, service.evaluate(ORGANIZATION_ID).status());
        assertTrue(service.hasFeature(ORGANIZATION_ID, "module.video-qc"));

        VerifiedLicense expired = license(NOW.minusSeconds(10), NOW, 10, List.of("module.video-qc"));
        assertEquals(EffectiveLicenseStatus.EXPIRED, serviceWith(repository, value -> expired).evaluate(ORGANIZATION_ID).status());
        assertEquals(EffectiveLicenseStatus.EXPIRED, serviceWith(repository, value -> license(NOW.minusSeconds(10), NOW.minusSeconds(1), 10, List.of())).evaluate(ORGANIZATION_ID).status());
        assertTrue(!serviceWith(repository, value -> expired).hasFeature(ORGANIZATION_ID, "module.video-qc"));

        VerifiedLicense future = license(NOW.plusSeconds(1), NOW.plusSeconds(10), 10, List.of("module.video-qc"));
        LicenseManagementService futureService = serviceWith(repository, value -> future);
        assertEquals(EffectiveLicenseStatus.INVALID, futureService.evaluate(ORGANIZATION_ID).status());
        assertEquals(null, futureService.entitlements(ACTOR).maxUsers());
        assertEquals(List.of(), futureService.entitlements(ACTOR).licenseFeatures());
        assertTrue(!futureService.hasFeature(ORGANIZATION_ID, "module.video-qc"));

        repository.stored = null;
        LicenseManagementService unlicensedService = serviceWith(repository, value -> active);
        assertEquals(EffectiveLicenseStatus.UNLICENSED, unlicensedService.evaluate(ORGANIZATION_ID).status());
        assertTrue(!unlicensedService.hasFeature(ORGANIZATION_ID, "module.video-qc"));
    }

    @Test
    void perpetualLicenseIsActiveAndNonActiveFeaturesAreFalse() {
        FakeLicenseRepository repository = new FakeLicenseRepository();
        repository.stored = stored("perpetual");
        LicenseManagementService service = service(repository,
                value -> new VerifiedLicense(1, UUID.randomUUID(), INSTALLATION_ID, LicenseType.PERPETUAL,
                        NOW.minusSeconds(1), null, 10, List.of("feature.one"), "Ed25519"), 0);
        assertEquals(EffectiveLicenseStatus.ACTIVE, service.evaluate(ORGANIZATION_ID).status());
        assertTrue(service.hasFeature(ORGANIZATION_ID, "feature.one"));
        assertTrue(!service.hasFeature(ORGANIZATION_ID, "feature.two"));
    }

    @Test
    void capacityAtLimitSucceedsAndAboveLimitLeavesPreviousLicenseUntouched() {
        FakeLicenseRepository repository = new FakeLicenseRepository();
        repository.stored = stored("old");
        FakeUserRepository users = new FakeUserRepository();
        users.activeCount = 2;
        VerifiedLicense candidate = license(NOW.minusSeconds(1), NOW.plusSeconds(100), 2, List.of());
        LicenseManagementService service = service(repository, value -> candidate, users.activeCount);
        service.activate(ACTOR, "candidate");
        assertEquals("candidate", repository.stored.signedLicense());

        users.activeCount = 3;
        repository.stored = stored("old");
        LicenseManagementService rejecting = service(repository, value -> candidate, users.activeCount);
        assertEquals("LICENSE_USER_LIMIT_EXCEEDED",
                assertThrows(LicenseException.class, () -> rejecting.activate(ACTOR, "candidate")).code());
        assertEquals("old", repository.stored.signedLicense());
    }

    @Test
    void installationMismatchIsRejectedBeforePersistence() {
        FakeLicenseRepository repository = new FakeLicenseRepository();
        repository.stored = stored("old");
        VerifiedLicense wrongInstallation = new VerifiedLicense(1, UUID.randomUUID(), UUID.randomUUID(),
                LicenseType.PERPETUAL, NOW.minusSeconds(1), null, 1, List.of(), "Ed25519");
        LicenseManagementService service = service(repository, value -> wrongInstallation, 0);
        assertEquals("LICENSE_INSTALLATION_MISMATCH",
                assertThrows(LicenseException.class, () -> service.activate(ACTOR, "candidate")).code());
        assertEquals("old", repository.stored.signedLicense());
    }

    @Test
    void activeUsersFromAnotherOrganizationDoNotConsumeCapacity() {
        FakeLicenseRepository repository = new FakeLicenseRepository();
        repository.stored = stored("active");
        FakeUserRepository users = new FakeUserRepository();
        users.organizationCounts.put(ORGANIZATION_ID, 1L);
        users.organizationCounts.put(UUID.randomUUID(), 100L);
        LicenseManagementService service = new LicenseManagementService(repository,
                value -> license(NOW.minusSeconds(1), NOW.plusSeconds(100), 2, List.of()),
                new FakeInstallationRepository(), users, Clock.fixed(NOW, ZoneOffset.UTC));

        service.requireCapacityForAdditionalActiveUser(ORGANIZATION_ID);
    }

    private LicenseManagementService service(FakeLicenseRepository repository, LicenseVerifier verifier, long activeCount) {
        return new LicenseManagementService(repository, verifier, new FakeInstallationRepository(),
                new FakeUserRepository(activeCount), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private LicenseManagementService serviceWith(FakeLicenseRepository repository, LicenseVerifier verifier) {
        return service(repository, verifier, 0);
    }

    private static VerifiedLicense license(Instant issuedAt, Instant expiresAt, int maxUsers, List<String> features) {
        return new VerifiedLicense(1, UUID.randomUUID(), INSTALLATION_ID, LicenseType.SUBSCRIPTION,
                issuedAt, expiresAt, maxUsers, features, "Ed25519");
    }

    private static LicenseRepository.StoredLicense stored(String value) {
        return new LicenseRepository.StoredLicense(INSTALLATION_ID, value, NOW, NOW);
    }

    private static final class FakeInstallationRepository implements InstallationRepository {
        private final Installation installation = new Installation(INSTALLATION_ID, ORGANIZATION_ID,
                "Local", "OTHER", "0.1.0", Installation.Status.UNLICENSED, NOW, null);

        @Override public Installation findCurrentByOrganizationId(UUID organizationId) { return installation; }
        @Override public Installation save(Installation value) { return value; }
    }

    private static final class FakeLicenseRepository implements LicenseRepository {
        private StoredLicense stored;
        @Override public Optional<StoredLicense> findByInstallationId(UUID installationId) { return Optional.ofNullable(stored); }
        @Override public StoredLicense save(UUID installationId, String signedLicense, Instant activatedAt, Instant updatedAt) {
            stored = new StoredLicense(installationId, signedLicense, activatedAt, updatedAt);
            return stored;
        }
        @Override public void deleteByInstallationId(UUID installationId) { stored = null; }
    }

    private static class FakeUserRepository implements UserRepository {
        private long activeCount;
        private final Map<UUID, Long> organizationCounts = new HashMap<>();
        FakeUserRepository() { }
        FakeUserRepository(long activeCount) { this.activeCount = activeCount; }
        @Override public long countEffectiveActiveUsers(UUID organizationId) {
            return organizationCounts.getOrDefault(organizationId, activeCount);
        }
        @Override public PageResult findAllByOrganizationId(UUID id, int page, int size, User.Status status) { return new PageResult(List.of(), 0); }
        @Override public Optional<UserRecord> findByOrganizationIdAndUserId(UUID id, UUID userId) { return Optional.empty(); }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public UserRecord create(User user, UUID id, Set<String> roles) { throw new UnsupportedOperationException(); }
        @Override public UserRecord update(UUID id, UUID userId, String displayName, User.Status status, Instant updatedAt) { throw new UnsupportedOperationException(); }
        @Override public UserRecord replaceRoles(UUID id, UUID userId, Set<String> roles) { throw new UnsupportedOperationException(); }
        @Override public Set<String> findUnknownSystemRoleCodes(Set<String> roles) { return Set.of(); }
        @Override public List<RoleView> findSystemRoles() { return List.of(); }
        @Override public boolean hasEffectiveActiveAdmin(UUID id, UUID excludedUserId) { return true; }
    }
}
