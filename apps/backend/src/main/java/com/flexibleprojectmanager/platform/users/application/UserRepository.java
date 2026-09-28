package com.flexibleprojectmanager.platform.users.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import com.flexibleprojectmanager.platform.users.domain.User;
import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;

public interface UserRepository {
    PageResult findAllByOrganizationId(UUID organizationId, int page, int size, User.Status status);
    Optional<UserRecord> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    boolean existsByEmail(String email);
    UserRecord create(User user, UUID organizationId, Set<String> roles);
    UserRecord update(UUID organizationId, UUID userId, String displayName, User.Status status, Instant updatedAt);
    UserRecord replaceRoles(UUID organizationId, UUID userId, Set<String> roles);
    Set<String> findUnknownSystemRoleCodes(Set<String> roles);
    List<RoleView> findSystemRoles();
    boolean hasEffectiveActiveAdmin(UUID organizationId, UUID excludedUserId);

    record PageResult(List<UserRecord> items, long total) {}
    record UserRecord(User user, Set<String> roles, OrganizationMember.Status membershipStatus) {}
}
