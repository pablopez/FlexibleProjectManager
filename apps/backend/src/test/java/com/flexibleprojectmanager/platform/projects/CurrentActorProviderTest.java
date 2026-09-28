package com.flexibleprojectmanager.platform.projects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.flexibleprojectmanager.platform.authentication.application.AuthenticationIdentity;
import com.flexibleprojectmanager.platform.authentication.application.AuthenticationRepository;
import com.flexibleprojectmanager.platform.projects.infrastructure.SpringSecurityCurrentActorProvider;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

class CurrentActorProviderTest {
    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void rechecksActiveUserMembershipAndOrganizationBeforeProjectsUseTheActor() {
        UUID userId = UUID.randomUUID();
        AuthenticationRepository repository = mock(AuthenticationRepository.class);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(identity(userId, "ACTIVE", "ACTIVE", "ACTIVE")));
        authenticate(userId);

        CurrentActor actor = new SpringSecurityCurrentActorProvider(repository).currentActor();

        assertEquals(userId, actor.userId());
    }

    @Test
    void rejectsDisabledUserMembershipOrOrganizationEvenWithAValidJwt() {
        UUID userId = UUID.randomUUID();
        AuthenticationRepository repository = mock(AuthenticationRepository.class);
        authenticate(userId);
        SpringSecurityCurrentActorProvider provider = new SpringSecurityCurrentActorProvider(repository);
        for (String[] statuses : List.of(
                new String[] { "DISABLED", "ACTIVE", "ACTIVE" },
                new String[] { "ACTIVE", "DISABLED", "ACTIVE" },
                new String[] { "ACTIVE", "ACTIVE", "DISABLED" })) {
            when(repository.findByUserId(userId)).thenReturn(Optional.of(identity(userId, statuses[0], statuses[1], statuses[2])));
            assertThrows(AuthorizationException.class, provider::currentActor);
        }
    }

    private void authenticate(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));
    }

    private AuthenticationIdentity identity(UUID userId, String userStatus, String memberStatus, String organizationStatus) {
        return new AuthenticationIdentity(userId, "user@example.com", "hash", "User", userStatus,
                UUID.randomUUID(), "Organization", organizationStatus, UUID.randomUUID(), memberStatus,
                List.of("USER"), List.of("projects:read"));
    }
}
