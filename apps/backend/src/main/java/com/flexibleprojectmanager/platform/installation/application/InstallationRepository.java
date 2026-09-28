package com.flexibleprojectmanager.platform.installation.application;

import java.util.UUID;
import com.flexibleprojectmanager.platform.installation.domain.Installation;

public interface InstallationRepository {
    Installation findCurrentByOrganizationId(UUID organizationId);
    Installation save(Installation installation);
}
