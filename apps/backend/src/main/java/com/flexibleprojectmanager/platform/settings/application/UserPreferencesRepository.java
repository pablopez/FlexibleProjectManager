package com.flexibleprojectmanager.platform.settings.application;

import java.util.Optional;
import java.util.UUID;

import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;

public interface UserPreferencesRepository {
    Optional<UserPreferences> findByUserId(UUID userId);
    UserPreferences save(UserPreferences preferences);
}
