package com.flexibleprojectmanager.platform.users.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(UUID id, String email, String displayName, String status,
                               List<String> roles, Instant createdAt, Instant updatedAt) {}
    public record UserListResponse(List<UserResponse> items, int page, int size, long total, int totalPages) {}
    public record CreateUserRequest(@Email @NotBlank String email,
                                    @NotBlank @Size(max = 200) String displayName,
                                    @NotBlank @Size(min = 8) String password,
                                    @NotEmpty List<String> roles) {}
    public static final class UpdateUserRequest {
        private String displayName;
        private String status;
        private boolean displayNameSupplied;
        private boolean statusSupplied;

        @com.fasterxml.jackson.annotation.JsonSetter(value = "displayName", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
        public void setDisplayName(String value) { displayName = value; displayNameSupplied = true; }
        @com.fasterxml.jackson.annotation.JsonSetter(value = "status", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
        public void setStatus(String value) { status = value; statusSupplied = true; }
        public String displayName() { return displayName; }
        public String status() { return status; }
        public boolean displayNameSupplied() { return displayNameSupplied; }
        public boolean statusSupplied() { return statusSupplied; }
    }
    public record UpdateUserRolesRequest(@NotEmpty List<String> roles) {}
    public record RoleResponse(String code, String name, String description, List<String> permissions) {}
    public record RoleListResponse(List<RoleResponse> items) {}
}
