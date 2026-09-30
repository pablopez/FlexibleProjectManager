package com.flexibleprojectmanager.platform.users.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.flexibleprojectmanager.platform.users.domain.User;

@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private User.Status status;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {}

    private UserJpaEntity(User user) {
        this.id = user.id(); this.email = user.email(); this.passwordHash = user.passwordHash();
        this.displayName = user.displayName(); this.status = user.status();
        this.createdAt = user.createdAt(); this.updatedAt = user.updatedAt();
    }

    static UserJpaEntity from(User user) { return new UserJpaEntity(user); }

    User toDomain() {
        return new User(id, email, passwordHash, displayName, status, createdAt, updatedAt);
    }

    void apply(User user) {
        this.displayName = user.displayName(); this.status = user.status(); this.updatedAt = user.updatedAt();
    }

    public String displayName() { return displayName; }
    public UUID id() { return id; }
}
