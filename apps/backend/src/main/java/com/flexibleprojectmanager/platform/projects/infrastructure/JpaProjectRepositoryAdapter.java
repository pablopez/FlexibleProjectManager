package com.flexibleprojectmanager.platform.projects.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.projects.application.ProjectManagementUseCase;
import com.flexibleprojectmanager.platform.projects.application.ProjectRepository;
import com.flexibleprojectmanager.platform.projects.domain.Project;

@Repository
public class JpaProjectRepositoryAdapter implements ProjectRepository {
    private static final Sort PROJECT_ORDER = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final SpringDataProjectRepository repository;

    public JpaProjectRepositoryAdapter(SpringDataProjectRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Project project) {
        repository.save(ProjectJpaEntity.from(project));
    }

    @Override
    public Optional<Project> find(UUID organizationId, UUID projectId) {
        return repository.findByOrganizationIdAndId(organizationId, projectId).map(ProjectJpaEntity::toDomain);
    }

    @Override
    public ProjectManagementUseCase.PageResult findAll(UUID organizationId, int page, int size, Project.Status status) {
        PageRequest request = PageRequest.of(page, size, PROJECT_ORDER);
        Page<ProjectJpaEntity> result = status == null
                ? repository.findByOrganizationId(organizationId, request)
                : repository.findByOrganizationIdAndStatus(organizationId, status, request);
        return new ProjectManagementUseCase.PageResult(
                result.getContent().stream().map(ProjectJpaEntity::toDomain).toList(), result.getTotalElements());
    }

    @Override
    public void update(Project project) {
        repository.save(ProjectJpaEntity.from(project));
    }
}
