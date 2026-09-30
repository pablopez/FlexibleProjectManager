package com.flexibleprojectmanager.platform.setup.infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.setup.application.SetupData;
import com.flexibleprojectmanager.platform.setup.application.SetupRepository;
import com.flexibleprojectmanager.platform.setup.application.SystemAlreadyInitializedException;

@Repository
public class JdbcSetupRepository implements SetupRepository {
    private final JdbcTemplate jdbc;
    private final boolean postgresql;

    public JdbcSetupRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        try (var connection = dataSource.getConnection()) {
            this.postgresql = "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Unable to determine database vendor for setup persistence", exception);
        }
    }

    @Override
    public boolean isInitialized() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_initialization WHERE singleton_id = 1", Integer.class);
        return count != null && count > 0;
    }

    @Override
    public void persist(SetupData data) {
        int inserted = jdbc.update("INSERT INTO system_initialization (singleton_id, initialized_at) VALUES (1, ?) ON CONFLICT (singleton_id) DO NOTHING",
                databaseValue(data.initializedAt()));
        if (inserted != 1) {
            throw new SystemAlreadyInitializedException();
        }

        jdbc.update("INSERT INTO organizations (id, name, status, created_at, updated_at) VALUES (?, ?, 'ACTIVE', ?, ?)",
                databaseValue(data.organizationId()), data.organizationName(), databaseValue(data.organizationCreatedAt()),
                databaseValue(data.organizationCreatedAt()));
        jdbc.update("INSERT INTO installations (id, organization_id, name, platform, application_version, status, created_at) VALUES (?, ?, ?, ?, ?, 'UNLICENSED', ?)",
                databaseValue(data.installationId()), databaseValue(data.organizationId()), data.installationName(), data.platform(),
                data.applicationVersion(), databaseValue(data.installationCreatedAt()));
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)",
                databaseValue(data.userId()), data.email(), data.passwordHash(), data.displayName(), databaseValue(data.userCreatedAt()),
                databaseValue(data.userCreatedAt()));
        jdbc.update("INSERT INTO organization_members (id, user_id, organization_id, status, joined_at) VALUES (?, ?, ?, 'ACTIVE', ?)",
                databaseValue(data.memberId()), databaseValue(data.userId()), databaseValue(data.organizationId()), databaseValue(data.memberJoinedAt()));

        Map<String, UUID> roleIds = new HashMap<>();
        for (String role : data.roles()) {
            UUID id = UUID.randomUUID();
            roleIds.put(role, id);
            jdbc.update("INSERT INTO roles (id, code, system_defined) VALUES (?, ?, ?)", databaseValue(id), role, databaseBoolean(true));
        }
        Map<String, UUID> permissionIds = new HashMap<>();
        for (String permission : data.permissions()) {
            UUID id = UUID.randomUUID();
            permissionIds.put(permission, id);
            jdbc.update("INSERT INTO permissions (id, code) VALUES (?, ?)", databaseValue(id), permission);
        }
        for (var assignment : data.rolePermissions()) {
            for (String permission : assignment.permissions()) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                        databaseValue(roleIds.get(assignment.role())), databaseValue(permissionIds.get(permission)));
            }
        }
        jdbc.update("INSERT INTO member_roles (member_id, role_id) VALUES (?, ?)", databaseValue(data.memberId()), databaseValue(roleIds.get("ADMIN")));
    }

    private Object databaseValue(Object value) {
        if (postgresql && value instanceof Instant instant) return instant.atOffset(ZoneOffset.UTC);
        if (postgresql) return value;
        if (value instanceof UUID || value instanceof Instant) return value.toString();
        return value;
    }

    private Object databaseBoolean(boolean value) {
        return postgresql ? value : (value ? 1 : 0);
    }
}
