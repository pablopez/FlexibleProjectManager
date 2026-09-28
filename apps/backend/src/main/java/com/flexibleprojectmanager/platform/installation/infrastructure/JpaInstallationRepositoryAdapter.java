package com.flexibleprojectmanager.platform.installation.infrastructure;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.flexibleprojectmanager.platform.installation.application.InstallationInvariantException;
import com.flexibleprojectmanager.platform.installation.application.InstallationNotFoundException;
import com.flexibleprojectmanager.platform.installation.application.InstallationRepository;
import com.flexibleprojectmanager.platform.installation.domain.Installation;

@Repository
public class JpaInstallationRepositoryAdapter implements InstallationRepository {
    private final SpringDataInstallationRepository repository;
    public JpaInstallationRepositoryAdapter(SpringDataInstallationRepository repository) { this.repository = repository; }
    @Override public Installation findCurrentByOrganizationId(UUID organizationId) {
        var installations = repository.findAllByOrganizationId(organizationId);
        if (installations.isEmpty()) throw new InstallationNotFoundException();
        if (installations.size() != 1) throw new InstallationInvariantException();
        return installations.get(0).toDomain();
    }
    @Override public Installation save(Installation installation) { return repository.save(InstallationJpaEntity.from(installation)).toDomain(); }
}
