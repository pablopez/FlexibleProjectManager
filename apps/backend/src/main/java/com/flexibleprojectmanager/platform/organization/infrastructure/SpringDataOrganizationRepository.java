package com.flexibleprojectmanager.platform.organization.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrganizationRepository extends JpaRepository<OrganizationJpaEntity, UUID> {
}
