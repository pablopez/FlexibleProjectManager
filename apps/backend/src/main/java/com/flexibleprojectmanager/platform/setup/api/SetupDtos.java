package com.flexibleprojectmanager.platform.setup.api;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SetupDtos {
    private SetupDtos() {
    }

    public record InitializeSetupRequest(
            @NotNull @Valid SetupOrganizationRequest organization,
            @NotNull @Valid SetupInstallationRequest installation,
            @NotNull @Valid SetupAdministratorRequest administrator) {
    }

    public record SetupOrganizationRequest(
            @NotBlank @Size(max = 200) String name) {
    }

    public record SetupInstallationRequest(
            @NotBlank @Size(max = 200) String name) {
    }

    public record SetupAdministratorRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 200) String displayName,
            @NotBlank @Size(min = 8) String password) {
    }

    public record SetupStatusResponse(boolean initialized) {
    }

    public record SetupInitializationResponse(
            boolean initialized,
            OrganizationSummary organization,
            InstallationResponse installation,
            SetupAdministratorResponse administrator) {
    }

    public record OrganizationSummary(UUID id, String name) {
    }

    public record InstallationResponse(
            UUID id,
            UUID organizationId,
            String name,
            String platform,
            String applicationVersion,
            String status,
            Instant createdAt,
            Instant lastSeenAt) {
    }

    public record SetupAdministratorResponse(UUID id, String email, String displayName) {
    }

}
