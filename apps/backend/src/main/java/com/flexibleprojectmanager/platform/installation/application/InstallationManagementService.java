package com.flexibleprojectmanager.platform.installation.application;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.installation.domain.Installation;
import com.flexibleprojectmanager.platform.audit.application.AuditConstants;
import com.flexibleprojectmanager.platform.audit.application.AuditEvents;
import com.flexibleprojectmanager.platform.audit.application.AuditRecorder;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

import static com.flexibleprojectmanager.platform.installation.application.InstallationManagementUseCase.InstallationView;

@Service
public class InstallationManagementService implements InstallationManagementUseCase {
    private final InstallationRepository repository;
    private final AuditRecorder audit;
    public InstallationManagementService(InstallationRepository repository) { this(repository, event -> {}); }
    @Autowired public InstallationManagementService(InstallationRepository repository, AuditRecorder audit) { this.repository = repository; this.audit = audit; }

    @Override @Transactional(readOnly = true)
    public InstallationView get(CurrentActor actor) { actor.requirePermission("organization:read"); return view(find(actor)); }

    @Override @Transactional
    public InstallationView update(CurrentActor actor, UpdateInstallationCommand command) {
        actor.requirePermission("organization:update");
        if (command == null || !command.nameSupplied()) throw new IllegalArgumentException("Installation name is required.");
        Installation current = find(actor);
        if (command.name() == null || command.name().trim().isBlank() || command.name().trim().length() > 200) throw new IllegalArgumentException("Installation name is invalid.");
        Installation saved = repository.save(current.rename(command.name().trim()));
        if (!java.util.Objects.equals(current.name(), saved.name())) AuditEvents.record(audit, actor, "INSTALLATION_UPDATED", AuditConstants.INSTALLATION, saved.id(), com.flexibleprojectmanager.platform.audit.domain.AuditMetadata.of("name"), java.time.Instant.now());
        return view(saved);
    }

    private Installation find(CurrentActor actor) { return repository.findCurrentByOrganizationId(actor.organizationId()); }
    private InstallationView view(Installation value) { return new InstallationView(value.id(), value.organizationId(), value.name(), value.platform(), value.applicationVersion(), value.status().name(), value.createdAt(), value.lastSeenAt()); }
}
