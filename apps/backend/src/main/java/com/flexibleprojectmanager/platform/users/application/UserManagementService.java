package com.flexibleprojectmanager.platform.users.application;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.shared.application.security.PasswordHasher;
import com.flexibleprojectmanager.platform.users.application.CreateUserUseCase.CreateUserCommand;
import com.flexibleprojectmanager.platform.users.application.UserManagementUseCase.UpdateUserCommand;
import com.flexibleprojectmanager.platform.users.domain.User;
import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;
import com.flexibleprojectmanager.platform.audit.application.AuditConstants;
import com.flexibleprojectmanager.platform.audit.application.AuditEvents;
import com.flexibleprojectmanager.platform.audit.application.AuditRecorder;

@Service
public class UserManagementService implements CreateUserUseCase, UserManagementUseCase, ListRolesUseCase {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final UserCapacityPolicy capacityPolicy;
    private final AuditRecorder audit;

    public UserManagementService(UserRepository repository, PasswordHasher passwordHasher, UserCapacityPolicy capacityPolicy) {
        this(repository, passwordHasher, Clock.systemUTC(), capacityPolicy, event -> {});
    }

    UserManagementService(UserRepository repository, PasswordHasher passwordHasher, Clock clock, UserCapacityPolicy capacityPolicy) {
        this(repository, passwordHasher, clock, capacityPolicy, event -> {});
    }

    @Autowired
    public UserManagementService(UserRepository repository, PasswordHasher passwordHasher, Clock clock, UserCapacityPolicy capacityPolicy, AuditRecorder audit) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.capacityPolicy = capacityPolicy;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult list(CurrentActor actor, int page, int size, User.Status status) {
        actor.requirePermission("users:read");
        validatePage(page, size);
        UserRepository.PageResult result = repository.findAllByOrganizationId(actor.organizationId(), page, size, status);
        return new PageResult(result.items().stream().map(this::view).toList(), page, size, result.total(),
                (int) Math.ceil((double) result.total() / size));
    }

    @Override
    @Transactional(readOnly = true)
    public UserView get(CurrentActor actor, UUID userId) {
        actor.requirePermission("users:read");
        return repository.findByOrganizationIdAndUserId(actor.organizationId(), userId)
                .map(this::view).orElseThrow(UserNotFoundException::new);
    }

    @Override
    @Transactional
    public UserView create(CurrentActor actor, CreateUserCommand command) {
        actor.requirePermission("users:create");
        if (command == null) throw new IllegalArgumentException("User data is required.");
        String email = normalizeEmail(command.email());
        String displayName = normalizeDisplayName(command.displayName());
        validatePassword(command.password());
        Set<String> roles = validateRoles(command.roles());
        if (repository.existsByEmail(email)) throw new UserEmailAlreadyExistsException();
        capacityPolicy.requireCapacityForAdditionalActiveUser(actor.organizationId());
        Instant now = Instant.now(clock);
        User user = User.create(UUID.randomUUID(), email, passwordHasher.hash(command.password()), displayName, now);
        UserRepository.UserRecord created = repository.create(user, actor.organizationId(), roles);
        AuditEvents.record(audit, actor, "USER_CREATED", AuditConstants.USER, user.id(), null, now);
        return view(created);
    }

    @Override
    @Transactional
    public UserView update(CurrentActor actor, UUID userId, UpdateUserCommand command) {
        // Local MVP invariant check and mutation share this application transaction;
        // this does not claim strong concurrent serialization.
        actor.requirePermission("users:update");
        if (command == null || (!command.displayNameSupplied() && !command.statusSupplied())) {
            throw new IllegalArgumentException("At least one user field is required.");
        }
        UserRepository.UserRecord existing = find(actor.organizationId(), userId);
        String displayName = command.displayNameSupplied() ? normalizeDisplayName(command.displayName()) : null;
        User.Status status = command.statusSupplied() ? command.status() : null;
        if (command.statusSupplied() && status == null) throw new IllegalArgumentException("Status is required.");
        if (status == User.Status.DISABLED && existing.user().status() == User.Status.ACTIVE
                && existing.membershipStatus() == OrganizationMember.Status.ACTIVE
                && existing.roles().contains("ADMIN")
                && !repository.hasEffectiveActiveAdmin(actor.organizationId(), userId)) {
            throw new LastActiveAdminRequiredException();
        }
        if (status == User.Status.ACTIVE && existing.user().status() == User.Status.DISABLED
                && existing.membershipStatus() == OrganizationMember.Status.ACTIVE) {
            capacityPolicy.requireCapacityForAdditionalActiveUser(actor.organizationId());
        }
        Instant now = Instant.now(clock);
        UserRepository.UserRecord updated = repository.update(actor.organizationId(), userId, displayName, status, now);
        if (command.displayNameSupplied() && !java.util.Objects.equals(existing.user().displayName(), updated.user().displayName())) {
            AuditEvents.record(audit, actor, "USER_UPDATED", AuditConstants.USER, userId, com.flexibleprojectmanager.platform.audit.domain.AuditMetadata.of("displayName"), now);
        }
        if (command.statusSupplied() && existing.user().status() != updated.user().status()) {
            String action = updated.user().status() == User.Status.DISABLED ? "USER_DISABLED" : "USER_REACTIVATED";
            AuditEvents.record(audit, actor, action, AuditConstants.USER, userId, null, now);
        }
        return view(updated);
    }

    @Override
    @Transactional
    public UserView replaceRoles(CurrentActor actor, UUID userId, List<String> requestedRoles) {
        actor.requirePermission("users:update");
        UserRepository.UserRecord existing = find(actor.organizationId(), userId);
        Set<String> roles = validateRoles(requestedRoles);
        if (existing.user().status() == User.Status.ACTIVE
                && existing.membershipStatus() == OrganizationMember.Status.ACTIVE
                && existing.roles().contains("ADMIN") && !roles.contains("ADMIN")
                && !repository.hasEffectiveActiveAdmin(actor.organizationId(), userId)) {
            throw new LastActiveAdminRequiredException();
        }
        UserRepository.UserRecord updated = repository.replaceRoles(actor.organizationId(), userId, roles);
        if (!existing.roles().equals(updated.roles())) AuditEvents.record(audit, actor, "USER_ROLES_CHANGED", AuditConstants.USER, userId, null, Instant.now(clock));
        return view(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleView> list(CurrentActor actor) {
        actor.requirePermission("users:read");
        return repository.findSystemRoles();
    }

    private UserRepository.UserRecord find(UUID organizationId, UUID userId) {
        return repository.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow(UserNotFoundException::new);
    }

    private UserView view(UserRepository.UserRecord record) {
        return new UserView(record.user().id(), record.user().email(), record.user().displayName(), record.user().status(),
                record.roles().stream().sorted().toList(), record.user().createdAt(), record.user().updatedAt());
    }

    private Set<String> validateRoles(List<String> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) throw new IllegalArgumentException("At least one role is required.");
        Set<String> roles = new HashSet<>(requestedRoles);
        if (roles.size() != requestedRoles.size()) throw new IllegalArgumentException("Roles must be unique.");
        if (!repository.findUnknownSystemRoleCodes(roles).isEmpty()) throw new InvalidUserRoleException();
        return Set.copyOf(roles);
    }

    private String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("Email is required.");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) throw new IllegalArgumentException("Email is invalid.");
        return normalized;
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isBlank()) throw new IllegalArgumentException("Display name is required.");
        String normalized = displayName.trim();
        if (normalized.length() > 200) throw new IllegalArgumentException("Display name is too long.");
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password is invalid.");
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("Invalid pagination.");
    }
}
