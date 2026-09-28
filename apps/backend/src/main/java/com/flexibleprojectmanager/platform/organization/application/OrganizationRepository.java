package com.flexibleprojectmanager.platform.organization.application;

import java.util.Optional;
import java.util.UUID;

import com.flexibleprojectmanager.platform.organization.domain.Organization;

public interface OrganizationRepository {
    Optional<Organization> findById(UUID organizationId);
    Organization save(Organization organization);
}
