package com.flexibleprojectmanager.platform.organization.api;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class OrganizationDtos {
    private OrganizationDtos() {}
    public record OrganizationResponse(UUID id, String name, String slug, String status, Instant createdAt, Instant updatedAt) {}
    public static final class UpdateOrganizationRequest {
        private String name; private String slug; private boolean nameSupplied; private boolean slugSupplied;
        @JsonSetter(value = "name", nulls = Nulls.SET) public void setName(String value) { name = value; nameSupplied = true; }
        @JsonSetter(value = "slug", nulls = Nulls.SET) public void setSlug(String value) { slug = value; slugSupplied = true; }
        public String name() { return name; } public String slug() { return slug; }
        public boolean nameSupplied() { return nameSupplied; } public boolean slugSupplied() { return slugSupplied; }
    }
}
