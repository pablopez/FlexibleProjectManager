package com.flexibleprojectmanager.platform.installation.application;

import java.time.Instant;
import java.util.UUID;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface InstallationManagementUseCase {
    InstallationView get(CurrentActor actor);
    InstallationView update(CurrentActor actor, UpdateInstallationCommand command);
    record UpdateInstallationCommand(String name, boolean nameSupplied) {}
    record InstallationView(UUID id, UUID organizationId, String name, String platform, String applicationVersion,
                            String status, Instant createdAt, Instant lastSeenAt) {}
}
