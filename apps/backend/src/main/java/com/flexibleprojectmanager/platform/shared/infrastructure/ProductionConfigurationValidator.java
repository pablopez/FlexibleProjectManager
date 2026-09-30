package com.flexibleprojectmanager.platform.shared.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.flexibleprojectmanager.platform.licensing.infrastructure.LicenseProperties;

/** Fails production startup before a deployment can run with development security defaults. */
@Component
@Profile("prod")
public class ProductionConfigurationValidator {
    public ProductionConfigurationValidator(
            @Value("${app.security.jwt-key-path}") String jwtKeyPath,
            @Value("${app.security.refresh-cookie-secure:false}") boolean secureCookie,
            @Value("${spring.datasource.url:}") String databaseUrl,
            @Value("${spring.datasource.username:}") String databaseUsername,
            @Value("${spring.datasource.password:}") String databasePassword,
            LicenseProperties licenseProperties) {
        require(secureCookie, "app.security.refresh-cookie-secure", "true");
        require(databaseUrl, "spring.datasource.url");
        require(databaseUsername, "spring.datasource.username");
        require(databasePassword, "spring.datasource.password");
        if (!Files.isRegularFile(Path.of(jwtKeyPath))) {
            throw new IllegalStateException("Missing required production configuration: app.security.jwt-key-path (external key file)");
        }
        if (licenseProperties.getTrustedKeys().isEmpty()
                || licenseProperties.getTrustedKeys().values().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalStateException("Missing required production configuration: app.licensing.trusted-keys");
        }
    }

    private static void require(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required production configuration: " + key);
        }
    }

    private static void require(boolean value, String key, String expected) {
        if (!value) {
            throw new IllegalStateException("Invalid production configuration: " + key + " must be " + expected);
        }
    }
}
