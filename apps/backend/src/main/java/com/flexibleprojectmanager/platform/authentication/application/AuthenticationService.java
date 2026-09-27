package com.flexibleprojectmanager.platform.authentication.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final AuthenticationRepository repository;
    private final PasswordVerifier passwordVerifier;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Duration refreshTokenLifetime;
    private final Clock clock;

    @Autowired
    public AuthenticationService(
            AuthenticationRepository repository,
            PasswordVerifier passwordVerifier,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenIssuer accessTokenIssuer,
            @org.springframework.beans.factory.annotation.Value("${app.security.refresh-token-lifetime:P30D}") Duration refreshTokenLifetime) {
        this(repository, passwordVerifier, refreshTokenGenerator, accessTokenIssuer, refreshTokenLifetime, Clock.systemUTC());
    }

    AuthenticationService(
            AuthenticationRepository repository,
            PasswordVerifier passwordVerifier,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenIssuer accessTokenIssuer,
            Duration refreshTokenLifetime,
            Clock clock) {
        this.repository = repository;
        this.passwordVerifier = passwordVerifier;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenLifetime = refreshTokenLifetime;
        this.clock = clock;
    }

    @Transactional
    public AuthenticatedSession login(String email, String password) {
        Optional<AuthenticationIdentity> candidate = repository.findByEmail(normalizeEmail(email));
        if (candidate.isEmpty()
                || !passwordVerifier.matches(password, candidate.get().passwordHash())
                || !candidate.get().eligible()) {
            throw new AuthenticationFailedException();
        }
        return createSession(candidate.get());
    }

    @Transactional
    public AuthenticatedSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        Instant now = Instant.now(clock);
        RefreshSession current = repository.findRefreshSessionByHash(refreshTokenGenerator.hash(rawRefreshToken))
                .orElseThrow(AuthenticationFailedException::new);
        if (!current.usableAt(now)) {
            throw new AuthenticationFailedException();
        }
        AuthenticationIdentity identity = repository.findByUserId(current.userId())
                .filter(AuthenticationIdentity::eligible)
                .orElseThrow(AuthenticationFailedException::new);

        String replacementRaw = refreshTokenGenerator.generate();
        UUID replacementId = UUID.randomUUID();
        RefreshSession replacement = new RefreshSession(
                replacementId, identity.userId(), refreshTokenGenerator.hash(replacementRaw), now,
                now.plus(refreshTokenLifetime), null, null);
        try {
            repository.saveRefreshSession(replacement);
            if (!repository.rotateRefreshSession(current.id(), replacementId, now)) {
                throw new AuthenticationFailedException();
            }
        } catch (DataAccessException exception) {
            throw new AuthenticationFailedException();
        }
        return new AuthenticatedSession(accessTokenIssuer.issue(identity), replacementRaw);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            repository.revokeRefreshSession(refreshTokenGenerator.hash(rawRefreshToken), Instant.now(clock));
        }
    }

    public CurrentUser currentUser(UUID userId) {
        AuthenticationIdentity identity = repository.findByUserId(userId)
                .filter(AuthenticationIdentity::eligible)
                .orElseThrow(AuthenticationFailedException::new);
        return new CurrentUser(identity);
    }

    private AuthenticatedSession createSession(AuthenticationIdentity identity) {
        Instant now = Instant.now(clock);
        String rawToken = refreshTokenGenerator.generate();
        repository.saveRefreshSession(new RefreshSession(
                UUID.randomUUID(), identity.userId(), refreshTokenGenerator.hash(rawToken), now,
                now.plus(refreshTokenLifetime), null, null));
        return new AuthenticatedSession(accessTokenIssuer.issue(identity), rawToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record AuthenticatedSession(AccessTokenIssuer.IssuedAccessToken accessToken, String refreshToken) {
    }

    public record CurrentUser(AuthenticationIdentity identity) {
    }

}
