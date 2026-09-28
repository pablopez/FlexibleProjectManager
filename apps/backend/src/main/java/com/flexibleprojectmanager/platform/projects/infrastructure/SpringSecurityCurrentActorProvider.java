package com.flexibleprojectmanager.platform.projects.infrastructure;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.flexibleprojectmanager.platform.authentication.application.AuthenticationIdentity;
import com.flexibleprojectmanager.platform.authentication.application.AuthenticationRepository;
import com.flexibleprojectmanager.platform.shared.application.security.AuthorizationException;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

@Component
public class SpringSecurityCurrentActorProvider implements CurrentActorProvider {
    private final AuthenticationRepository authenticationRepository;

    public SpringSecurityCurrentActorProvider(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    public CurrentActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthorizationException();
        }
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new AuthorizationException();
        }

        AuthenticationIdentity identity = authenticationRepository.findByUserId(userId)
                .filter(AuthenticationIdentity::eligible)
                .orElseThrow(AuthorizationException::new);
        return new CurrentActor(identity.userId(), identity.organizationId(), identity.memberId(), identity.permissions());
    }
}
