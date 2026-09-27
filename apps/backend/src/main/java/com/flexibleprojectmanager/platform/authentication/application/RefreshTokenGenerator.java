package com.flexibleprojectmanager.platform.authentication.application;

public interface RefreshTokenGenerator {
    String generate();

    String hash(String rawToken);
}
