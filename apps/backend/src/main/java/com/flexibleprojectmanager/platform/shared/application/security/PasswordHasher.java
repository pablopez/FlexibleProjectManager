package com.flexibleprojectmanager.platform.shared.application.security;

public interface PasswordHasher {
    String hash(String rawPassword);
}
