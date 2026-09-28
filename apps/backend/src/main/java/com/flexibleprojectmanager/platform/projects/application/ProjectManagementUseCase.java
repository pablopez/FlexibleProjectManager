package com.flexibleprojectmanager.platform.projects.application;

import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.projects.domain.Project;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface ProjectManagementUseCase {
    PageResult list(CurrentActor actor, int page, int size, Project.Status status);
    Project get(CurrentActor actor, UUID projectId);
    Project update(CurrentActor actor, UUID projectId, String name, boolean nameSupplied, String description, boolean descriptionSupplied);
    Project archive(CurrentActor actor, UUID projectId);
    Project restore(CurrentActor actor, UUID projectId);

    record PageResult(List<Project> items, long total) {}
}
