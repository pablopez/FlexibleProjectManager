package com.flexibleprojectmanager.platform.projects.application;

import java.util.UUID;

import com.flexibleprojectmanager.platform.projects.domain.Project;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

/** Stable application boundary for creating a generic project. */
public interface CreateProjectUseCase {
    Project create(CurrentActor actor, CreateProjectCommand command);

    record CreateProjectCommand(String name, String description) {}
}
