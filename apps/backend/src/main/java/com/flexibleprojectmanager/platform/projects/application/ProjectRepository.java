package com.flexibleprojectmanager.platform.projects.application;

import java.util.Optional;
import java.util.UUID;

import com.flexibleprojectmanager.platform.projects.domain.Project;

public interface ProjectRepository {
    void save(Project project);
    Optional<Project> find(UUID organizationId, UUID projectId);
    ProjectManagementUseCase.PageResult findAll(UUID organizationId, int page, int size, Project.Status status);
    void update(Project project);
}
