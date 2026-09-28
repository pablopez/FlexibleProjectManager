package com.flexibleprojectmanager.platform.setup.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.flexibleprojectmanager.platform.shared.application.security.PasswordHasher;

@Configuration
public class PasswordHashConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return passwordEncoder::encode;
    }
}
