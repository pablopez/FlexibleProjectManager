package com.flexibleprojectmanager.platform.users.application;

import java.util.UUID;

public interface UserCapacityPolicy {
    void requireCapacityForAdditionalActiveUser(UUID organizationId);
}
