package com.flexibleprojectmanager.platform.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.projects.application.CreateProjectUseCase.CreateProjectCommand;
import com.flexibleprojectmanager.platform.projects.application.ProjectApplicationService;
import com.flexibleprojectmanager.platform.projects.application.ProjectManagementUseCase;
import com.flexibleprojectmanager.platform.projects.application.ProjectRepository;
import com.flexibleprojectmanager.platform.projects.application.ProjectNotFoundException;
import com.flexibleprojectmanager.platform.projects.domain.Project;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

class ProjectApplicationServiceTest {
    @Test
    void createsAGenericProjectWhenInvokedDirectlyThroughTheApplicationBoundary() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        var useCase = new ProjectApplicationService(repository, Clock.fixed(now, ZoneOffset.UTC));
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CurrentActor actor = new CurrentActor(userId, organizationId, UUID.randomUUID(), java.util.List.of("projects:create"));
        Project project = useCase.create(actor, new CreateProjectCommand("  Generic project  ", null));

        assertNotNull(project.id());
        assertEquals(organizationId, project.organizationId());
        assertEquals(userId, project.createdBy());
        assertEquals("Generic project", project.name());
        assertEquals(Project.Status.ACTIVE, project.status());
        assertEquals(project, repository.saved);
    }

    @Test
    void patchSemanticsKeepReplaceAndClearDescription() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        var service = new ProjectApplicationService(repository, Clock.systemUTC());
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CurrentActor actor = new CurrentActor(userId, organizationId, UUID.randomUUID(), java.util.List.of("projects:create", "projects:update"));
        Project original = service.create(actor, new CreateProjectCommand("Project", "original"));

        Project nameOnly = service.update(actor, original.id(), "Renamed", true, null, false);
        assertEquals("original", nameOnly.description());
        Project replaced = service.update(actor, original.id(), null, false, "replacement", true);
        assertEquals("replacement", replaced.description());
        Project cleared = service.update(actor, original.id(), null, false, null, true);
        assertEquals(null, cleared.description());
        assertThrows(IllegalArgumentException.class, () -> service.update(actor, original.id(), null, false, null, false));
    }

    @Test
    void isolatesProjectsByActorOrganization() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        var service = new ProjectApplicationService(repository, Clock.systemUTC());
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CurrentActor owner = new CurrentActor(userId, organizationId, UUID.randomUUID(), java.util.List.of("projects:create"));
        Project project = service.create(owner, new CreateProjectCommand("Project", null));
        CurrentActor actorFromOtherOrganization = new CurrentActor(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), java.util.List.of("projects:read"));

        assertThrows(ProjectNotFoundException.class, () -> service.get(actorFromOtherOrganization, project.id()));
    }

    private static final class InMemoryProjectRepository implements ProjectRepository {
        private Project saved;

        @Override public void save(Project project) { saved = project; }
        @Override public Optional<Project> find(UUID organizationId, UUID projectId) {
            return saved != null && saved.organizationId().equals(organizationId) && saved.id().equals(projectId)
                    ? Optional.of(saved) : Optional.empty();
        }
        @Override public ProjectManagementUseCase.PageResult findAll(UUID organizationId, int page, int size, Project.Status status) {
            return new ProjectManagementUseCase.PageResult(saved == null ? java.util.List.of() : java.util.List.of(saved), saved == null ? 0 : 1);
        }
        @Override public void update(Project project) { saved = project; }
    }
}
