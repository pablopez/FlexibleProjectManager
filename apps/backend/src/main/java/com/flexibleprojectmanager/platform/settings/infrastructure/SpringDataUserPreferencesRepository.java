package com.flexibleprojectmanager.platform.settings.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserPreferencesRepository extends JpaRepository<UserPreferencesJpaEntity, UUID> {
}
