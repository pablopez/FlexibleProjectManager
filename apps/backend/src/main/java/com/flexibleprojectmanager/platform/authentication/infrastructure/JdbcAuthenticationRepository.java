package com.flexibleprojectmanager.platform.authentication.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.authentication.application.AuthenticationIdentity;
import com.flexibleprojectmanager.platform.authentication.application.AuthenticationRepository;
import com.flexibleprojectmanager.platform.authentication.application.RefreshSession;

@Repository
public class JdbcAuthenticationRepository implements AuthenticationRepository {
    private final JdbcTemplate jdbc;

    public JdbcAuthenticationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AuthenticationIdentity> findByEmail(String email) {
        return findIdentity("u.email = ?", email);
    }

    @Override
    public Optional<AuthenticationIdentity> findByUserId(UUID userId) {
        return findIdentity("u.id = ?", userId.toString());
    }

    private Optional<AuthenticationIdentity> findIdentity(String predicate, String value) {
        List<AuthenticationIdentity> identities = jdbc.query(
                "SELECT u.id, u.email, u.password_hash, u.display_name, u.status AS user_status, "
                        + "o.id AS organization_id, o.name AS organization_name, o.status AS organization_status, "
                        + "m.id AS member_id, m.status AS member_status "
                        + "FROM users u JOIN organization_members m ON m.user_id = u.id "
                        + "JOIN organizations o ON o.id = m.organization_id WHERE " + predicate + " ORDER BY m.joined_at LIMIT 1",
                (rs, rowNum) -> identityWithoutRoles(rs), value);
        if (identities.isEmpty()) {
            return Optional.empty();
        }
        AuthenticationIdentity base = identities.getFirst();
        List<String> roles = jdbc.queryForList(
                "SELECT DISTINCT r.code FROM roles r JOIN member_roles mr ON mr.role_id = r.id WHERE mr.member_id = ? ORDER BY r.code",
                String.class, base.memberId().toString());
        List<String> permissions = jdbc.queryForList(
                "SELECT DISTINCT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id = p.id "
                        + "JOIN member_roles mr ON mr.role_id = rp.role_id WHERE mr.member_id = ? ORDER BY p.code",
                String.class, base.memberId().toString());
        return Optional.of(new AuthenticationIdentity(
                base.userId(), base.email(), base.passwordHash(), base.displayName(), base.userStatus(),
                base.organizationId(), base.organizationName(), base.organizationStatus(), base.memberId(),
                base.memberStatus(), roles, permissions));
    }

    private AuthenticationIdentity identityWithoutRoles(ResultSet rs) throws SQLException {
        return new AuthenticationIdentity(
                UUID.fromString(rs.getString("id")), rs.getString("email"), rs.getString("password_hash"),
                rs.getString("display_name"), rs.getString("user_status"), UUID.fromString(rs.getString("organization_id")),
                rs.getString("organization_name"), rs.getString("organization_status"), UUID.fromString(rs.getString("member_id")),
                rs.getString("member_status"), List.of(), List.of());
    }

    @Override
    public void saveRefreshSession(RefreshSession session) {
        jdbc.update("INSERT INTO refresh_sessions (id, user_id, token_hash, created_at, expires_at, revoked_at, replaced_by_token_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                session.id().toString(), session.userId().toString(), session.tokenHash(), session.createdAt().toString(),
                session.expiresAt().toString(), nullable(session.revokedAt()), nullable(session.replacedByTokenId()));
    }

    @Override
    public Optional<RefreshSession> findRefreshSessionByHash(String tokenHash) {
        List<RefreshSession> sessions = jdbc.query(
                "SELECT id, user_id, token_hash, created_at, expires_at, revoked_at, replaced_by_token_id FROM refresh_sessions WHERE token_hash = ?",
                (rs, rowNum) -> refreshSession(rs), tokenHash);
        return sessions.stream().findFirst();
    }

    @Override
    public boolean rotateRefreshSession(UUID currentId, UUID replacementId, Instant revokedAt) {
        return jdbc.update("UPDATE refresh_sessions SET revoked_at = ?, replaced_by_token_id = ? "
                        + "WHERE id = ? AND revoked_at IS NULL AND expires_at > ?",
                revokedAt.toString(), replacementId.toString(), currentId.toString(), revokedAt.toString()) == 1;
    }

    @Override
    public void revokeRefreshSession(String tokenHash, Instant revokedAt) {
        jdbc.update("UPDATE refresh_sessions SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL",
                revokedAt.toString(), tokenHash);
    }

    private RefreshSession refreshSession(ResultSet rs) throws SQLException {
        return new RefreshSession(
                UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("user_id")), rs.getString("token_hash"),
                Instant.parse(rs.getString("created_at")), Instant.parse(rs.getString("expires_at")),
                rs.getString("revoked_at") == null ? null : Instant.parse(rs.getString("revoked_at")),
                rs.getString("replaced_by_token_id") == null ? null : UUID.fromString(rs.getString("replaced_by_token_id")));
    }

    private String nullable(Instant value) {
        return value == null ? null : value.toString();
    }

    private String nullable(UUID value) {
        return value == null ? null : value.toString();
    }
}
