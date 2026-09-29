package com.flexibleprojectmanager.platform.settings.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class UserPreferencesDtos {
    private UserPreferencesDtos() { }

    public record UserSettingsResponse(String language, String theme) { }

    public static final class UpdateUserSettingsRequest {
        private String language;
        private String theme;
        private boolean languageSupplied;
        private boolean themeSupplied;

        @JsonSetter(value = "language", nulls = Nulls.SET)
        public void setLanguage(String language) { this.language = language; languageSupplied = true; }

        @JsonSetter(value = "theme", nulls = Nulls.SET)
        public void setTheme(String theme) { this.theme = theme; themeSupplied = true; }

        public String language() { return language; }
        public String theme() { return theme; }
        public boolean languageSupplied() { return languageSupplied; }
        public boolean themeSupplied() { return themeSupplied; }
    }
}
