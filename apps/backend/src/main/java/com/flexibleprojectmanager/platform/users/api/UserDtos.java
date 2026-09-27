package com.flexibleprojectmanager.platform.users.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(UUID id, String email, String displayName, String status,
                               List<String> roles, Instant createdAt, Instant updatedAt) {}
    public record UserListResponse(List<UserResponse> items, int page, int size, long total, int totalPages) {}
    public record CreateUserRequest(@Email @NotBlank String email, @NotBlank @Size(max = 200) String displayName,
                                    @NotBlank @Size(min = 8) String password, @NotEmpty List<String> roles) {}
    public record UpdateUserRequest(@Size(min = 1, max = 200) String displayName, String status) {}
    public record UpdateUserRolesRequest(@NotEmpty List<String> roles) {}
    public record RoleResponse(String code, String name, String description, List<String> permissions) {}
    public record RoleListResponse(List<RoleResponse> items) {}
}
