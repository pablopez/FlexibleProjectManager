package com.flexibleprojectmanager.platform.users.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.users.application.UserManagementRepository;

@Repository
public class JdbcUserManagementRepository implements UserManagementRepository {
    private final JdbcTemplate jdbc;
    public JdbcUserManagementRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public PageResult<UserData> findUsers(int page, int size, String status) {
        String filter = status == null ? "" : " WHERE u.status = ?";
        Object[] args = status == null ? new Object[] { size, page * size } : new Object[] { status, size, page * size };
        List<UserData> items = jdbc.query("SELECT u.id,u.email,u.display_name,u.status,u.created_at,u.updated_at FROM users u" + filter + " ORDER BY u.email LIMIT ? OFFSET ?", args, (rs, n) -> mapUser(rs));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM users u" + filter, status == null ? new Object[0] : new Object[] { status }, Long.class);
        return new PageResult<>(items, total == null ? 0 : total);
    }

    @Override public Optional<UserData> findUser(UUID id) {
        List<UserData> users = jdbc.query("SELECT id,email,display_name,status,created_at,updated_at FROM users WHERE id = ?", (rs, n) -> mapUser(rs), id.toString());
        return users.stream().findFirst();
    }

    @Override public UserData createUser(String email, String displayName, String hash, List<String> roles) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO users (id,email,password_hash,display_name,status,created_at,updated_at) VALUES (?,?,?,?,'ACTIVE',?,?)", id.toString(), email, hash, displayName, now, now);
        String organization = jdbc.queryForObject("SELECT id FROM organizations ORDER BY created_at LIMIT 1", String.class);
        jdbc.update("INSERT INTO organization_members (id,user_id,organization_id,status,joined_at) VALUES (?,?,?,'ACTIVE',?)",
                UUID.randomUUID().toString(), id.toString(), organization, now);
        assignRoles(id, roles);
        return findUser(id).orElseThrow();
    }

    @Override public UserData updateUser(UUID id, String displayName, String status) {
        if (displayName == null && status == null) throw new IllegalArgumentException("At least one field is required.");
        jdbc.update("UPDATE users SET display_name = COALESCE(?, display_name), status = COALESCE(?, status), updated_at = ? WHERE id = ?", displayName, status, Instant.now(), id.toString());
        return findUser(id).orElseThrow();
    }

    @Override public UserData replaceRoles(UUID id, List<String> roles) {
        jdbc.update("DELETE FROM member_roles WHERE member_id IN (SELECT id FROM organization_members WHERE user_id = ?)", id.toString());
        assignRoles(id, roles);
        return findUser(id).orElseThrow();
    }

    private void assignRoles(UUID userId, List<String> roles) {
        String member = jdbc.queryForObject("SELECT id FROM organization_members WHERE user_id = ?", String.class, userId.toString());
        for (String role : roles) {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE code = ?", Integer.class, role);
            if (exists == null || exists == 0) throw new IllegalArgumentException("Unknown role.");
            jdbc.update("INSERT INTO member_roles(member_id,role_id) SELECT ?,id FROM roles WHERE code = ?", member, role);
        }
    }

    @Override public List<RoleData> findRoles() {
        return jdbc.query("SELECT id,code FROM roles ORDER BY code", (rs, n) -> new RoleData(rs.getString("code"), displayName(rs.getString("code")), description(rs.getString("code")), jdbc.queryForList("SELECT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id=p.id WHERE rp.role_id=? ORDER BY p.code", String.class, rs.getString("id"))));
    }

    private UserData mapUser(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        List<String> roles = jdbc.queryForList("SELECT r.code FROM roles r JOIN member_roles mr ON mr.role_id=r.id JOIN organization_members om ON om.id=mr.member_id WHERE om.user_id=? ORDER BY r.code", String.class, id.toString());
        return new UserData(id, rs.getString("email"), rs.getString("display_name"), rs.getString("status"), roles, Instant.parse(rs.getString("created_at")), Instant.parse(rs.getString("updated_at")));
    }
    private static String displayName(String code) { return code.substring(0, 1) + code.substring(1).toLowerCase(); }
    private static String description(String code) { return switch (code) { case "ADMIN" -> "Full platform access"; case "USER" -> "Standard user access"; default -> "Read-only platform access"; }; }
}
