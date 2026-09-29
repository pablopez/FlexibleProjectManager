package com.flexibleprojectmanager.platform.settings.domain;

import java.time.Instant;
import java.util.UUID;

public record UserPreferences(UUID userId, Language language, Theme theme, Instant updatedAt) {
    public static final Language DEFAULT_LANGUAGE = Language.EN;
    public static final Theme DEFAULT_THEME = Theme.LIGHT;

    public static UserPreferences defaults(UUID userId) {
        return new UserPreferences(userId, DEFAULT_LANGUAGE, DEFAULT_THEME, null);
    }
}
