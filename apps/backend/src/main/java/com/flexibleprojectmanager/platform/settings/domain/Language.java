package com.flexibleprojectmanager.platform.settings.domain;

public enum Language {
    EN("en"), ES("es");

    private final String apiValue;

    Language(String apiValue) { this.apiValue = apiValue; }

    public String apiValue() { return apiValue; }

    public static Language fromApiValue(String value) {
        for (Language language : values()) {
            if (language.apiValue.equals(value)) return language;
        }
        throw new IllegalArgumentException("Unsupported language.");
    }
}
