package com.flexibleprojectmanager.platform.authentication.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.flexibleprojectmanager.platform.authentication.application.AuthenticationIdentity;
import com.flexibleprojectmanager.platform.authentication.application.AuthenticationFailedException;
import com.flexibleprojectmanager.platform.authentication.application.AuthenticationService;
import com.flexibleprojectmanager.platform.authentication.api.AuthenticationDtos.CurrentUserResponse;
import com.flexibleprojectmanager.platform.authentication.api.AuthenticationDtos.LoginRequest;
import com.flexibleprojectmanager.platform.authentication.api.AuthenticationDtos.OrganizationSummary;
import com.flexibleprojectmanager.platform.authentication.api.AuthenticationDtos.TokenResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    static final String REFRESH_COOKIE = "fpm_refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthenticationService authenticationService;
    private final boolean secureCookie;
    private final Duration refreshTokenLifetime;

    public AuthenticationController(
            AuthenticationService authenticationService,
            @Value("${app.security.refresh-cookie-secure:false}") boolean secureCookie,
            @Value("${app.security.refresh-token-lifetime:P30D}") Duration refreshTokenLifetime) {
        this.authenticationService = authenticationService;
        this.secureCookie = secureCookie;
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationService.AuthenticatedSession session = authenticationService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()))
                .body(tokenResponse(session));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthenticationService.AuthenticatedSession session = authenticationService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()))
                .body(tokenResponse(session));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        authenticationService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie())
                .build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationFailedException();
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationFailedException();
        }

        java.util.UUID userId;
        try {
            userId = java.util.UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException();
        }

        AuthenticationIdentity identity = authenticationService.currentUser(userId).identity();
        return new CurrentUserResponse(
                identity.userId(), identity.email(), identity.displayName(),
                new OrganizationSummary(identity.organizationId(), identity.organizationName()),
                identity.roles(), identity.permissions());
    }

    private TokenResponse tokenResponse(AuthenticationService.AuthenticatedSession session) {
        return new TokenResponse(session.accessToken().token(), session.accessToken().lifetime().toSeconds());
    }

    private String refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(refreshTokenLifetime)
                .build().toString();
    }

    private String expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build().toString();
    }
}
