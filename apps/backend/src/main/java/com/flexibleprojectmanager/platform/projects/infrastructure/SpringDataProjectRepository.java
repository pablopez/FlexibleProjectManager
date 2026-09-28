package com.flexibleprojectmanager.platform.projects.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flexibleprojectmanager.platform.projects.domain.Project;

public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, String> {
    Page<ProjectJpaEntity> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<ProjectJpaEntity> findByOrganizationIdAndStatus(UUID organizationId, Project.Status status, Pageable pageable);
    java.util.Optional<ProjectJpaEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
