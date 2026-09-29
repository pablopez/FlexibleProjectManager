package com.flexibleprojectmanager.platform.settings.application;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;

public interface UserPreferencesUseCase {
    UserPreferences get(CurrentActor actor);
    UserPreferences update(CurrentActor actor, UpdatePreferencesCommand command);

    record UpdatePreferencesCommand(String language, boolean languageSupplied,
                                    String theme, boolean themeSupplied) {
        public boolean isEmpty() { return !languageSupplied && !themeSupplied; }
    }
}
