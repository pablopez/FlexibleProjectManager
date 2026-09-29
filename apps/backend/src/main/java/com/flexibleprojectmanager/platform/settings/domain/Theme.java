package com.flexibleprojectmanager.platform.settings.domain;

public enum Theme {
    LIGHT("light"), DARK("dark"), SYSTEM("system");

    private final String apiValue;

    Theme(String apiValue) { this.apiValue = apiValue; }

    public String apiValue() { return apiValue; }

    public static Theme fromApiValue(String value) {
        for (Theme theme : values()) {
            if (theme.apiValue.equals(value)) return theme;
        }
        throw new IllegalArgumentException("Unsupported theme.");
    }
}
