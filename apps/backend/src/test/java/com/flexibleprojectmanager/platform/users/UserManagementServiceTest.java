package com.flexibleprojectmanager.platform.users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.users.application.CreateUserUseCase.CreateUserCommand;
import com.flexibleprojectmanager.platform.users.application.UserManagementService;
import com.flexibleprojectmanager.platform.users.application.UserRepository;
import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;
import com.flexibleprojectmanager.platform.users.domain.User;

class UserManagementServiceTest {
    @Test
    void createUsesCurrentActorOrganizationAndHashesPasswordWithoutHttp() {
        UUID organizationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        CapturingRepository repository = new CapturingRepository();
        UserManagementService service = new UserManagementService(repository, raw -> "bcrypt-hash");

        var result = service.create(new CurrentActor(actorId, organizationId, UUID.randomUUID(), List.of("users:create")),
                new CreateUserCommand(" New@Example.com ", " New User ", "password123", List.of("USER")));

        assertEquals(organizationId, repository.organizationId);
        assertEquals("new@example.com", result.email());
        assertEquals("bcrypt-hash", repository.created.passwordHash());
        assertEquals(User.Status.ACTIVE, repository.created.status());
        assertEquals(Set.of("USER"), repository.createdRoles);
    }

    private static final class CapturingRepository implements UserRepository {
        private UUID organizationId;
        private User created;
        private Set<String> createdRoles;

        @Override public PageResult findAllByOrganizationId(UUID id, int page, int size, User.Status status) { return new PageResult(List.of(), 0); }
        @Override public java.util.Optional<UserRecord> findByOrganizationIdAndUserId(UUID id, UUID userId) { return java.util.Optional.empty(); }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public UserRecord create(User user, UUID id, Set<String> roles) { organizationId = id; created = user; createdRoles = roles; return new UserRecord(user, roles, OrganizationMember.Status.ACTIVE); }
        @Override public UserRecord update(UUID id, UUID userId, String displayName, User.Status status, Instant updatedAt) { throw new UnsupportedOperationException(); }
        @Override public UserRecord replaceRoles(UUID id, UUID userId, Set<String> roles) { throw new UnsupportedOperationException(); }
        @Override public Set<String> findUnknownSystemRoleCodes(Set<String> roles) { return Set.of(); }
        @Override public List<com.flexibleprojectmanager.platform.users.application.RoleView> findSystemRoles() { return List.of(); }
        @Override public boolean hasEffectiveActiveAdmin(UUID id, UUID excludedUserId) { return true; }
    }
}
