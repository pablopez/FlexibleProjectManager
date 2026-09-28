package com.flexibleprojectmanager.platform.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.organization.application.OrganizationManagementService;
import com.flexibleprojectmanager.platform.organization.application.OrganizationNotFoundException;
import com.flexibleprojectmanager.platform.organization.application.OrganizationRepository;
import com.flexibleprojectmanager.platform.organization.application.OrganizationManagementUseCase.UpdateOrganizationCommand;
import com.flexibleprojectmanager.platform.organization.domain.Organization;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

class OrganizationManagementServiceTest {
    @Test
    void readsAndUpdatesTheOrganizationFromCurrentActor() {
        UUID organizationId = UUID.randomUUID();
        CapturingRepository repository = new CapturingRepository(new Organization(organizationId, "Old", null, Organization.Status.ACTIVE, Instant.EPOCH, Instant.EPOCH));
        OrganizationManagementService service = new OrganizationManagementService(repository);
        CurrentActor actor = new CurrentActor(UUID.randomUUID(), organizationId, UUID.randomUUID(), List.of("organization:read", "organization:update"));

        assertEquals(organizationId, service.get(actor).id());
        assertEquals("New", service.update(actor, new UpdateOrganizationCommand(" New ", true, "new", true)).name());
        assertEquals(organizationId, repository.updated.id());
    }

    @Test
    void permissionsAndMissingOrganizationAreEnforced() {
        UUID organizationId = UUID.randomUUID();
        OrganizationManagementService service = new OrganizationManagementService(new CapturingRepository(null));
        assertThrows(AuthorizationException.class, () -> service.get(new CurrentActor(UUID.randomUUID(), organizationId, UUID.randomUUID(), List.of())));
        assertThrows(OrganizationNotFoundException.class, () -> service.get(new CurrentActor(UUID.randomUUID(), organizationId, UUID.randomUUID(), List.of("organization:read"))));
        assertThrows(AuthorizationException.class, () -> service.update(new CurrentActor(UUID.randomUUID(), organizationId, UUID.randomUUID(), List.of("organization:read")), new UpdateOrganizationCommand("x", true, null, false)));
    }

    private static final class CapturingRepository implements OrganizationRepository {
        private Organization value;
        private Organization updated;
        CapturingRepository(Organization value) { this.value = value; }
        @Override public Optional<Organization> findById(UUID organizationId) { return Optional.ofNullable(value).filter(item -> item.id().equals(organizationId)); }
        @Override public Organization save(Organization organization) { updated = organization; value = organization; return organization; }
    }
}
