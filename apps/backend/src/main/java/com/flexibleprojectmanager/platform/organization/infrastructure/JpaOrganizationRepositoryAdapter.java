package com.flexibleprojectmanager.platform.organization.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.organization.application.OrganizationRepository;
import com.flexibleprojectmanager.platform.organization.domain.Organization;

@Repository
public class JpaOrganizationRepositoryAdapter implements OrganizationRepository {
    private final SpringDataOrganizationRepository repository;
    public JpaOrganizationRepositoryAdapter(SpringDataOrganizationRepository repository) { this.repository = repository; }
    @Override public java.util.Optional<Organization> findById(UUID organizationId) { return repository.findById(organizationId).map(OrganizationJpaEntity::toDomain); }
    @Override public Organization save(Organization organization) { return repository.save(OrganizationJpaEntity.from(organization)).toDomain(); }
}
