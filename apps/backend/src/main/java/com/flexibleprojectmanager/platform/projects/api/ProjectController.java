package com.flexibleprojectmanager.platform.projects.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.flexibleprojectmanager.platform.projects.api.ProjectDtos.CreateProjectRequest;
import com.flexibleprojectmanager.platform.projects.api.ProjectDtos.ProjectListResponse;
import com.flexibleprojectmanager.platform.projects.api.ProjectDtos.ProjectResponse;
import com.flexibleprojectmanager.platform.projects.api.ProjectDtos.UpdateProjectRequest;
import com.flexibleprojectmanager.platform.projects.application.CreateProjectUseCase;
import com.flexibleprojectmanager.platform.projects.application.ProjectManagementUseCase;
import com.flexibleprojectmanager.platform.projects.domain.Project;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final CreateProjectUseCase createProjectUseCase;
    private final ProjectManagementUseCase projectManagementUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ProjectController(CreateProjectUseCase createProjectUseCase, ProjectManagementUseCase projectManagementUseCase,
            CurrentActorProvider currentActorProvider) {
        this.createProjectUseCase = createProjectUseCase;
        this.projectManagementUseCase = projectManagementUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    public ProjectListResponse list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) Project.Status status) {
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("Project pagination is invalid.");
        CurrentActor actor = currentActorProvider.currentActor();
        var result = projectManagementUseCase.list(actor, page, size, status);
        return new ProjectListResponse(result.items().stream().map(this::response).toList(), page, size,
                result.total(), (int) Math.ceil((double) result.total() / size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('projects:create')")
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        CurrentActor actor = currentActorProvider.currentActor();
        actor.requirePermission("projects:create");
        return response(createProjectUseCase.create(actor, new CreateProjectUseCase.CreateProjectCommand(
                request.name(), request.description())));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAuthority('projects:read')")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return response(projectManagementUseCase.get(currentActorProvider.currentActor(), projectId));
    }

    @PatchMapping("/{projectId}")
    @PreAuthorize("hasAuthority('projects:update')")
    public ProjectResponse update(@PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return response(projectManagementUseCase.update(currentActorProvider.currentActor(), projectId,
                request.name(), request.nameSupplied(), request.description(), request.descriptionSupplied()));
    }

    @PostMapping("/{projectId}/archive")
    @PreAuthorize("hasAuthority('projects:archive')")
    public ProjectResponse archive(@PathVariable UUID projectId) {
        return response(projectManagementUseCase.archive(currentActorProvider.currentActor(), projectId));
    }

    @PostMapping("/{projectId}/restore")
    @PreAuthorize("hasAuthority('projects:archive')")
    public ProjectResponse restore(@PathVariable UUID projectId) {
        return response(projectManagementUseCase.restore(currentActorProvider.currentActor(), projectId));
    }

    private ProjectResponse response(Project project) {
        return new ProjectResponse(project.id(), project.organizationId(), project.name(), project.description(), project.status().name(),
                project.createdBy(), project.createdAt(), project.updatedAt());
    }

}
