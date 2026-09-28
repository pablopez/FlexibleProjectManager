package com.flexibleprojectmanager.platform.organization.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.organization.domain.Organization;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

import static com.flexibleprojectmanager.platform.organization.application.OrganizationManagementUseCase.OrganizationView;

@Service
public class OrganizationManagementService implements OrganizationManagementUseCase {
    private final OrganizationRepository repository;
    private final Clock clock;

    @Autowired
    public OrganizationManagementService(OrganizationRepository repository) {
        this(repository, Clock.systemUTC());
    }

    OrganizationManagementService(OrganizationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationView get(CurrentActor actor) {
        actor.requirePermission("organization:read");
        return view(find(actor));
    }

    @Override
    @Transactional
    public OrganizationView update(CurrentActor actor, UpdateOrganizationCommand command) {
        actor.requirePermission("organization:update");
        if (command == null || (!command.nameSupplied() && !command.slugSupplied())) {
            throw new IllegalArgumentException("At least one organization field is required.");
        }
        Organization current = find(actor);
        String name = command.nameSupplied() ? normalizeName(command.name()) : current.name();
        String slug = command.slugSupplied() ? normalizeSlug(command.slug()) : current.slug();
        return view(repository.save(current.update(name, slug, Instant.now(clock))));
    }

    private Organization find(CurrentActor actor) {
        return repository.findById(actor.organizationId()).orElseThrow(OrganizationNotFoundException::new);
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isBlank() || name.trim().length() > 200) {
            throw new IllegalArgumentException("Organization name is invalid.");
        }
        return name.trim();
    }

    private String normalizeSlug(String slug) {
        if (slug == null) return null;
        String normalized = slug.trim();
        if (normalized.length() > 100) throw new IllegalArgumentException("Organization slug is invalid.");
        return normalized;
    }

    private OrganizationView view(Organization organization) {
        return new OrganizationView(organization.id(), organization.name(), organization.slug(), organization.status().name(),
                organization.createdAt(), organization.updatedAt());
    }
}
