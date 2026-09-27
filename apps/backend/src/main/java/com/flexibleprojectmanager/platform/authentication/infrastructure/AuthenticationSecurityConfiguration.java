package com.flexibleprojectmanager.platform.authentication.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.flexibleprojectmanager.platform.authentication.application.PasswordVerifier;
import com.flexibleprojectmanager.platform.authentication.application.RefreshTokenGenerator;

@Configuration
public class AuthenticationSecurityConfiguration {
    @Bean
    PasswordVerifier passwordVerifier(PasswordEncoder passwordEncoder) {
        return passwordEncoder::matches;
    }

    @Bean
    RefreshTokenGenerator refreshTokenGenerator() {
        SecureRandom random = new SecureRandom();
        return new RefreshTokenGenerator() {
            @Override
            public String generate() {
                byte[] bytes = new byte[32];
                random.nextBytes(bytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            }

            @Override
            public String hash(String rawToken) {
                try {
                    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
                } catch (java.security.NoSuchAlgorithmException exception) {
                    throw new IllegalStateException("SHA-256 is unavailable.", exception);
                }
            }
        };
    }
}
