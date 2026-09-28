package com.flexibleprojectmanager.platform.installation.application;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.installation.domain.Installation;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

import static com.flexibleprojectmanager.platform.installation.application.InstallationManagementUseCase.InstallationView;

@Service
public class InstallationManagementService implements InstallationManagementUseCase {
    private final InstallationRepository repository;
    @Autowired public InstallationManagementService(InstallationRepository repository) { this.repository = repository; }

    @Override @Transactional(readOnly = true)
    public InstallationView get(CurrentActor actor) { actor.requirePermission("organization:read"); return view(find(actor)); }

    @Override @Transactional
    public InstallationView update(CurrentActor actor, UpdateInstallationCommand command) {
        actor.requirePermission("organization:update");
        if (command == null || !command.nameSupplied()) throw new IllegalArgumentException("Installation name is required.");
        Installation current = find(actor);
        if (command.name() == null || command.name().trim().isBlank() || command.name().trim().length() > 200) throw new IllegalArgumentException("Installation name is invalid.");
        return view(repository.save(current.rename(command.name().trim())));
    }

    private Installation find(CurrentActor actor) { return repository.findCurrentByOrganizationId(actor.organizationId()); }
    private InstallationView view(Installation value) { return new InstallationView(value.id(), value.organizationId(), value.name(), value.platform(), value.applicationVersion(), value.status().name(), value.createdAt(), value.lastSeenAt()); }
}
