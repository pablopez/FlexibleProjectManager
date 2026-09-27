package com.flexibleprojectmanager.platform.authentication.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import com.flexibleprojectmanager.platform.authentication.application.AccessTokenIssuer;
import com.flexibleprojectmanager.platform.authentication.application.AuthenticationIdentity;

@Component
public class NimbusAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final Duration lifetime;

    public NimbusAccessTokenIssuer(
            JwtEncoder encoder,
            @Value("${app.security.access-token-lifetime:PT15M}") Duration lifetime) {
        this.encoder = encoder;
        this.lifetime = lifetime;
    }

    @Override
    public IssuedAccessToken issue(AuthenticationIdentity identity) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(identity.userId().toString())
                .claim("userId", identity.userId().toString())
                .claim("organizationId", identity.organizationId().toString())
                .claim("memberId", identity.memberId().toString())
                .claim("roles", identity.roles())
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .id(UUID.randomUUID().toString())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("fpm-local-rs256").build(), claims)).getTokenValue();
        return new IssuedAccessToken(token, lifetime);
    }
}
