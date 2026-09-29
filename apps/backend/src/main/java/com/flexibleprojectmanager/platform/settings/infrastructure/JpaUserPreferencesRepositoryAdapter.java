package com.flexibleprojectmanager.platform.settings.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.settings.application.UserPreferencesRepository;
import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;

@Repository
public class JpaUserPreferencesRepositoryAdapter implements UserPreferencesRepository {
    private final SpringDataUserPreferencesRepository repository;

    public JpaUserPreferencesRepositoryAdapter(SpringDataUserPreferencesRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserPreferences> findByUserId(UUID userId) {
        return repository.findById(userId).map(UserPreferencesJpaEntity::toDomain);
    }

    @Override
    public UserPreferences save(UserPreferences preferences) {
        return repository.save(UserPreferencesJpaEntity.from(preferences)).toDomain();
    }
}
