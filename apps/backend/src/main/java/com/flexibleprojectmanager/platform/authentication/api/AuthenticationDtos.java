package com.flexibleprojectmanager.platform.authentication.api;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthenticationDtos {
    private AuthenticationDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, long expiresIn) {
    }

    public record CurrentUserResponse(
            UUID id,
            String email,
            String displayName,
            OrganizationSummary organization,
            java.util.List<String> roles,
            java.util.List<String> permissions) {
    }

    public record OrganizationSummary(UUID id, String name) {
    }

    public record ErrorResponse(String code, String message, int status, Instant timestamp, String path) {
    }
}
