package com.flexibleprojectmanager.platform.installation.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInstallationRepository extends JpaRepository<InstallationJpaEntity, UUID> {
    List<InstallationJpaEntity> findAllByOrganizationId(UUID organizationId);
}
