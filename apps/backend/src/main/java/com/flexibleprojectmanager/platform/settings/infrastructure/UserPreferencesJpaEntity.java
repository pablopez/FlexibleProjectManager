package com.flexibleprojectmanager.platform.settings.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.flexibleprojectmanager.platform.settings.domain.Language;
import com.flexibleprojectmanager.platform.settings.domain.Theme;
import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;

@Entity
@Table(name = "user_preferences")
public class UserPreferencesJpaEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 2)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 6)
    private Theme theme;

    @Column(name = "updated_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant updatedAt;

    protected UserPreferencesJpaEntity() { }

    private UserPreferencesJpaEntity(UserPreferences value) {
        userId = value.userId();
        language = value.language();
        theme = value.theme();
        updatedAt = value.updatedAt();
    }

    static UserPreferencesJpaEntity from(UserPreferences value) { return new UserPreferencesJpaEntity(value); }

    UserPreferences toDomain() { return new UserPreferences(userId, language, theme, updatedAt); }
}
