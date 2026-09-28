package com.flexibleprojectmanager.platform.setup.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.shared.application.security.PasswordHasher;

@Service
public class SetupService {
    private static final String APPLICATION_VERSION = "0.1.0";
    private static final List<String> ROLES = List.of("ADMIN", "USER", "VIEWER");
    private static final List<String> PERMISSIONS = List.of(
            "users:read", "users:create", "users:update",
            "projects:read", "projects:create", "projects:update", "projects:archive",
            "organization:read", "organization:update",
            "license:read", "license:manage", "audit:read");
    private static final List<RolePermissionAssignment> ROLE_PERMISSIONS = List.of(
            new RolePermissionAssignment("ADMIN", PERMISSIONS),
            new RolePermissionAssignment("USER", List.of(
                    "projects:read", "projects:create", "projects:update", "organization:read")),
            new RolePermissionAssignment("VIEWER", List.of("projects:read", "organization:read")));

    private final SetupRepository repository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    @Autowired
    public SetupService(SetupRepository repository, PasswordHasher passwordHasher) {
        this(repository, passwordHasher, Clock.systemUTC());
    }

    SetupService(SetupRepository repository, PasswordHasher passwordHasher, Clock clock) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public boolean isInitialized() {
        return repository.isInitialized();
    }

    @Transactional
    public SetupResult initialize(SetupCommand command) {
        if (repository.isInitialized()) {
            throw new SystemAlreadyInitializedException();
        }

        Instant now = Instant.now(clock);
        SetupData data = new SetupData(
                UUID.randomUUID(), command.organizationName().trim(), now,
                UUID.randomUUID(), command.installationName().trim(), detectPlatform(), APPLICATION_VERSION, now,
                UUID.randomUUID(), command.email().trim().toLowerCase(Locale.ROOT), command.displayName().trim(),
                passwordHasher.hash(command.password()), now,
                UUID.randomUUID(), now, ROLES, PERMISSIONS, ROLE_PERMISSIONS, now);
        repository.persist(data);
        return new SetupResult(
                data.organizationId(), data.organizationName(),
                data.installationId(), data.installationName(), data.platform(),
                data.applicationVersion(), "UNLICENSED", data.installationCreatedAt(),
                data.userId(), data.email(), data.displayName());
    }

    private String detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "WINDOWS";
        if (os.contains("linux")) return "LINUX";
        return "OTHER";
    }

    public record SetupCommand(
            String organizationName, String installationName,
            String email, String displayName, String password) {
    }

    public record SetupResult(
            UUID organizationId,
            String organizationName,
            UUID installationId,
            String installationName,
            String platform,
            String applicationVersion,
            String installationStatus,
            Instant installationCreatedAt,
            UUID administratorId,
            String administratorEmail,
            String administratorDisplayName) {
    }

}
