package com.flexibleprojectmanager.platform.installation.api;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class InstallationDtos {
    private InstallationDtos() {}
    public record InstallationResponse(UUID id, UUID organizationId, String name, String platform, String applicationVersion, String status, Instant createdAt, Instant lastSeenAt) {}
    public static final class UpdateInstallationRequest {
        private String name; private boolean nameSupplied;
        @JsonSetter(value = "name", nulls = Nulls.SET) public void setName(String value) { name = value; nameSupplied = true; }
        public String name() { return name; } public boolean nameSupplied() { return nameSupplied; }
    }
}
