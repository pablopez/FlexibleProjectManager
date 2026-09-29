package com.flexibleprojectmanager.platform.users.infrastructure;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.users.application.RoleView;
import com.flexibleprojectmanager.platform.users.application.UserEmailAlreadyExistsException;
import com.flexibleprojectmanager.platform.users.application.UserRepository;
import com.flexibleprojectmanager.platform.users.domain.User;
import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository users;
    private final SpringDataOrganizationMemberRepository members;
    private final SpringDataRoleRepository roles;

    public JpaUserRepositoryAdapter(SpringDataUserRepository users,
                                    SpringDataOrganizationMemberRepository members,
                                    SpringDataRoleRepository roles) {
        this.users = users; this.members = members; this.roles = roles;
    }

    @Override
    public PageResult findAllByOrganizationId(UUID organizationId, int page, int size, User.Status status) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        var result = users.findByOrganizationId(organizationId, status, pageable);
        return new PageResult(result.getContent().stream().map(user -> record(organizationId, user)).toList(), result.getTotalElements());
    }

    @Override
    public java.util.Optional<UserRecord> findByOrganizationIdAndUserId(UUID organizationId, UUID userId) {
        return users.findByOrganizationIdAndUserId(organizationId, userId).map(user -> record(organizationId, user));
    }

    @Override public boolean existsByEmail(String email) { return users.existsByEmail(email); }

    @Override
    public long countEffectiveActiveUsers(UUID organizationId) {
        return users.countEffectiveActiveUsers(organizationId, User.Status.ACTIVE, OrganizationMember.Status.ACTIVE);
    }

    @Override
    public UserRecord create(User user, UUID organizationId, Set<String> roleCodes) {
        UserJpaEntity entity = UserJpaEntity.from(user);
        try {
            users.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new UserEmailAlreadyExistsException();
        }
        Set<RoleJpaEntity> roleEntities = roles.findByCodeInAndSystemDefinedTrue(roleCodes).stream().collect(Collectors.toSet());
        members.save(OrganizationMemberJpaEntity.create(user.id(), organizationId, user.createdAt(), roleEntities));
        return new UserRecord(user, roleCodes, OrganizationMember.Status.ACTIVE);
    }

    @Override
    public UserRecord update(UUID organizationId, UUID userId, String displayName, User.Status status, java.time.Instant updatedAt) {
        UserJpaEntity entity = users.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow();
        entity.apply(entity.toDomain().update(displayName, status, updatedAt));
        users.save(entity);
        return record(organizationId, entity);
    }

    @Override
    public UserRecord replaceRoles(UUID organizationId, UUID userId, Set<String> roleCodes) {
        UserJpaEntity entity = users.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow();
        OrganizationMemberJpaEntity member = members.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow();
        member.replaceRoles(roles.findByCodeInAndSystemDefinedTrue(roleCodes).stream().collect(Collectors.toSet()));
        members.save(member);
        return record(organizationId, entity);
    }

    @Override
    public Set<String> findUnknownSystemRoleCodes(Set<String> roleCodes) {
        Set<String> found = roles.findByCodeInAndSystemDefinedTrue(roleCodes).stream().map(RoleJpaEntity::code).collect(Collectors.toSet());
        return roleCodes.stream().filter(code -> !found.contains(code)).collect(Collectors.toSet());
    }

    @Override
    public List<RoleView> findSystemRoles() {
        return roles.findBySystemDefinedTrueOrderByCodeAsc().stream()
                .map(role -> new RoleView(role.code(), role.code(), description(role.code()),
                        role.permissions().stream().map(PermissionJpaEntity::code).sorted().toList()))
                .toList();
    }

    @Override
    public boolean hasEffectiveActiveAdmin(UUID organizationId, UUID excludedUserId) {
        return members.countOtherEffectiveActiveAdmins(organizationId, excludedUserId,
                OrganizationMember.Status.ACTIVE, User.Status.ACTIVE) > 0;
    }

    private UserRecord record(UUID organizationId, UserJpaEntity entity) {
        Set<String> roleCodes = members.findByOrganizationIdAndUserId(organizationId, entity.toDomain().id())
                .map(member -> member.roles().stream().map(RoleJpaEntity::code).collect(Collectors.toSet()))
                .orElseGet(Set::of);
        OrganizationMember.Status membershipStatus = members.findByOrganizationIdAndUserId(organizationId, entity.toDomain().id())
                .map(OrganizationMemberJpaEntity::status).orElse(OrganizationMember.Status.DISABLED);
        return new UserRecord(entity.toDomain(), roleCodes, membershipStatus);
    }

    private static String description(String code) {
        return switch (code) {
            case "ADMIN" -> "Full platform access";
            case "USER" -> "Standard user access";
            case "VIEWER" -> "Read-only platform access";
            default -> null;
        };
    }
}
