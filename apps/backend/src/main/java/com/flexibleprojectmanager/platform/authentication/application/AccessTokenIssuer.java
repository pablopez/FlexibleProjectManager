package com.flexibleprojectmanager.platform.authentication.application;

import java.time.Duration;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(AuthenticationIdentity identity);

    record IssuedAccessToken(String token, Duration lifetime) {
    }
}
