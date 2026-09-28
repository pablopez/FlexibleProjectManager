package com.flexibleprojectmanager.platform.projects.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.flexibleprojectmanager.platform.projects.domain.Project;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

@Service
public class ProjectApplicationService implements CreateProjectUseCase, ProjectManagementUseCase {
    private final ProjectRepository repository;
    private final Clock clock;

    @Autowired
    public ProjectApplicationService(ProjectRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public ProjectApplicationService(ProjectRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Project create(CurrentActor actor, CreateProjectCommand command) {
        actor.requirePermission("projects:create");
        Project project = Project.create(actor.organizationId(), actor.userId(), command.name(), command.description(), Instant.now(clock));
        repository.save(project);
        return project;
    }

    @Override public PageResult list(CurrentActor actor, int page, int size, Project.Status status) {
        actor.requirePermission("projects:read");
        return repository.findAll(actor.organizationId(), page, size, status);
    }

    @Override public Project get(CurrentActor actor, UUID projectId) {
        actor.requirePermission("projects:read");
        return find(actor.organizationId(), projectId);
    }

    @Override
    @Transactional
    public Project update(CurrentActor actor, UUID projectId, String name, boolean nameSupplied, String description, boolean descriptionSupplied) {
        actor.requirePermission("projects:update");
        if (!nameSupplied && !descriptionSupplied) throw new IllegalArgumentException("At least one project field is required.");
        if (nameSupplied && (name == null || name.isBlank())) throw new IllegalArgumentException("Project name is invalid.");
        Project updated = find(actor.organizationId(), projectId).update(name, nameSupplied, description, descriptionSupplied, Instant.now(clock));
        repository.update(updated);
        return updated;
    }

    @Override
    @Transactional
    public Project archive(CurrentActor actor, UUID projectId) {
        actor.requirePermission("projects:archive");
        Project current = find(actor.organizationId(), projectId);
        if (current.status() == Project.Status.ARCHIVED) throw new ProjectAlreadyArchivedException();
        Project updated = current.archive(Instant.now(clock));
        repository.update(updated);
        return updated;
    }

    @Override
    @Transactional
    public Project restore(CurrentActor actor, UUID projectId) {
        actor.requirePermission("projects:archive");
        Project current = find(actor.organizationId(), projectId);
        if (current.status() == Project.Status.ACTIVE) throw new ProjectAlreadyActiveException();
        Project updated = current.restore(Instant.now(clock));
        repository.update(updated);
        return updated;
    }

    private Project find(UUID organizationId, UUID projectId) {
        return repository.find(organizationId, projectId).orElseThrow(ProjectNotFoundException::new);
    }
}
