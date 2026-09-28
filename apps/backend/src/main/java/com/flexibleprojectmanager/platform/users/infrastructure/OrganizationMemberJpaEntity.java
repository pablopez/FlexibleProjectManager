package com.flexibleprojectmanager.platform.users.infrastructure;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.flexibleprojectmanager.platform.users.domain.OrganizationMember;

@Entity
@Table(name = "organization_members")
public class OrganizationMemberJpaEntity {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID id;
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationMember.Status status;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleJpaEntity> roles = new HashSet<>();

    protected OrganizationMemberJpaEntity() {}

    static OrganizationMemberJpaEntity create(UUID userId, UUID organizationId, Instant joinedAt, Set<RoleJpaEntity> roles) {
        OrganizationMemberJpaEntity member = new OrganizationMemberJpaEntity();
        member.id = UUID.randomUUID(); member.userId = userId; member.organizationId = organizationId;
        member.status = OrganizationMember.Status.ACTIVE; member.joinedAt = joinedAt; member.roles.addAll(roles);
        return member;
    }

    Set<RoleJpaEntity> roles() { return roles; }
    OrganizationMember.Status status() { return status; }
    UUID userId() { return userId; }
    UUID organizationId() { return organizationId; }
    void replaceRoles(Set<RoleJpaEntity> replacement) { roles.clear(); roles.addAll(replacement); }
}
