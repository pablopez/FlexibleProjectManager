package com.flexibleprojectmanager.platform.organization.application;

import java.util.UUID;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface OrganizationManagementUseCase {
    OrganizationView get(CurrentActor actor);
    OrganizationView update(CurrentActor actor, UpdateOrganizationCommand command);

    record UpdateOrganizationCommand(String name, boolean nameSupplied, String slug, boolean slugSupplied) {}
    record OrganizationView(UUID id, String name, String slug, String status, java.time.Instant createdAt, java.time.Instant updatedAt) {}
}
