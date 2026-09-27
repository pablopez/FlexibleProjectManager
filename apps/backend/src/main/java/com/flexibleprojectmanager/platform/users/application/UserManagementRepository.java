package com.flexibleprojectmanager.platform.users.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserManagementRepository {
    PageResult<UserData> findUsers(int page, int size, String status);
    Optional<UserData> findUser(UUID userId);
    UserData createUser(String email, String displayName, String passwordHash, List<String> roles);
    UserData updateUser(UUID userId, String displayName, String status);
    UserData replaceRoles(UUID userId, List<String> roles);
    List<RoleData> findRoles();

    record PageResult<T>(List<T> items, long total) {}
    record UserData(UUID id, String email, String displayName, String status,
                    List<String> roles, java.time.Instant createdAt, java.time.Instant updatedAt) {}
    record RoleData(String code, String name, String description, List<String> permissions) {}
}
