package com.flexibleprojectmanager.platform.organization.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.organization.domain.Organization;
import com.flexibleprojectmanager.platform.audit.application.AuditConstants;
import com.flexibleprojectmanager.platform.audit.application.AuditEvents;
import com.flexibleprojectmanager.platform.audit.application.AuditRecorder;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

import static com.flexibleprojectmanager.platform.organization.application.OrganizationManagementUseCase.OrganizationView;

@Service
public class OrganizationManagementService implements OrganizationManagementUseCase {
    private final OrganizationRepository repository;
    private final Clock clock;
    private final AuditRecorder audit;

    public OrganizationManagementService(OrganizationRepository repository) {
        this(repository, Clock.systemUTC(), event -> {});
    }

    OrganizationManagementService(OrganizationRepository repository, Clock clock) {
        this(repository, clock, event -> {});
    }

    @Autowired
    public OrganizationManagementService(OrganizationRepository repository, Clock clock, AuditRecorder audit) {
        this.repository = repository;
        this.clock = clock;
        this.audit = audit;
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
        Organization updated = current.update(name, slug, Instant.now(clock));
        Organization saved = repository.save(updated);
        var fields = new java.util.ArrayList<String>();
        if (command.nameSupplied() && !java.util.Objects.equals(current.name(), saved.name())) fields.add("name");
        if (command.slugSupplied() && !java.util.Objects.equals(current.slug(), saved.slug())) fields.add("slug");
        if (!fields.isEmpty()) AuditEvents.record(audit, actor, "ORGANIZATION_UPDATED", AuditConstants.ORGANIZATION, saved.id(), AuditEvents.fields(fields), saved.updatedAt());
        return view(saved);
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
