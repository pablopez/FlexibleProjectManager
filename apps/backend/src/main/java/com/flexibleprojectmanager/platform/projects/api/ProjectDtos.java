package com.flexibleprojectmanager.platform.projects.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record ProjectResponse(UUID id, UUID organizationId, String name, String description,
                                  String status, UUID createdBy, Instant createdAt, Instant updatedAt) {}
    public record ProjectListResponse(List<ProjectResponse> items, int page, int size, long total, int totalPages) {}
    public record CreateProjectRequest(@NotBlank @Size(max = 200) String name, @Size(max = 2000) String description) {}
    public static final class UpdateProjectRequest {
        private String name;
        private String description;
        private boolean nameSupplied;
        private boolean descriptionSupplied;

        @JsonSetter(value = "name", nulls = Nulls.SET)
        public void setName(String name) { this.name = name; this.nameSupplied = true; }

        @JsonSetter(value = "description", nulls = Nulls.SET)
        public void setDescription(String description) { this.description = description; this.descriptionSupplied = true; }

        public String name() { return name; }
        public String description() { return description; }
        public boolean nameSupplied() { return nameSupplied; }
        public boolean descriptionSupplied() { return descriptionSupplied; }
    }
}
