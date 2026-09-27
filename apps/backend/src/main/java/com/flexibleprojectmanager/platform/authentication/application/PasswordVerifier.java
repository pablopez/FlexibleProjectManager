package com.flexibleprojectmanager.platform.authentication.application;

public interface PasswordVerifier {
    boolean matches(String rawPassword, String passwordHash);
}
