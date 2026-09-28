package com.flexibleprojectmanager.platform.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class UserManagementSlice5IntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-users-slice5-" + UUID.randomUUID(), "test.sqlite");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    private String adminToken;
    private UUID createdUser;
    private UUID secondAdmin;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", () -> database.toString());
    }

    @BeforeAll
    void initialize() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        adminToken = login("admin@example.com", "password123");
    }

    @Test
    @Order(1)
    void rolesAndCreationUsePersistedPermissionsAndAllowAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/roles").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].permissions").isArray());

        MvcResult result = create("New@Example.com", "New User", "password123", "USER");
        createdUser = UUID.fromString(json(result, "id"));
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, createdUser.toString());
        org.junit.jupiter.api.Assertions.assertTrue(hash.startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertNotEquals("password123", hash);
        org.junit.jupiter.api.Assertions.assertEquals("new@example.com", json(result, "email"));
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, createdUser.toString()));
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM organization_members WHERE user_id = ?", String.class, createdUser.toString()));
        mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void rejectsUnknownNonSystemDuplicateAndEmptyRoles() throws Exception {
        UUID roleId = UUID.randomUUID();
        jdbc.update("INSERT INTO roles (id, code, system_defined) VALUES (?, ?, ?)", roleId.toString(), "CUSTOM", 0);
        createExpecting("unknown@example.com", "NOPE").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_USER_ROLE"));
        createExpecting("custom@example.com", "CUSTOM").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_USER_ROLE"));
        createExpecting("empty@example.com").andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/users/" + createdUser + "/roles").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"roles\":[\"USER\",\"USER\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/roles").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.items[?(@.code == 'CUSTOM')]").isEmpty());
    }

    @Test
    @Order(3)
    void rejectsDuplicateEmail() throws Exception {
        createExpecting("NEW@example.com", "USER").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @Order(4)
    void listsWithDefaultsFiltersAndValidation() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(25));
        mockMvc.perform(get("/api/v1/users?size=201").header("Authorization", bearer(adminToken))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/users?status=ACTIVE").header("Authorization", bearer(adminToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users?status=DISABLED").header("Authorization", bearer(adminToken))).andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void patchesOnlyAllowedFieldsAndReplacesRoles() throws Exception {
        mockMvc.perform(patch("/api/v1/users/" + createdUser).header("Authorization", bearer(adminToken)).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/users/" + createdUser).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"displayName\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/users/" + createdUser).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"status\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/users/" + createdUser).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"displayName\":\"Updated\",\"email\":\"changed@example.com\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Updated")).andExpect(jsonPath("$.email").value("new@example.com")).andExpect(jsonPath("$.roles[0]").value("USER"));
        mockMvc.perform(put("/api/v1/users/" + createdUser + "/roles").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"roles\":[\"VIEWER\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("VIEWER"));
        String viewerToken = login("new@example.com", "password123");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("VIEWER"))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    @Order(6)
    void isolatesOtherOrganizationUsers() throws Exception {
        UUID otherOrg = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO organizations (id,name,status,created_at,updated_at) VALUES (?,?,?, ?, ?)", otherOrg.toString(), "Other", "ACTIVE", now.toString(), now.toString());
        jdbc.update("INSERT INTO users (id,email,password_hash,display_name,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)", otherUser.toString(), "other@example.com", passwordEncoder.encode("password123"), "Other", "ACTIVE", now.toString(), now.toString());
        jdbc.update("INSERT INTO organization_members (id,user_id,organization_id,status,joined_at) VALUES (?,?,?,?,?)", member.toString(), otherUser.toString(), otherOrg.toString(), "ACTIVE", now.toString());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[?(@.id == '" + otherUser + "')]").isEmpty());
        mockMvc.perform(get("/api/v1/users/" + otherUser).header("Authorization", bearer(adminToken))).andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/users/" + otherUser).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"displayName\":\"Nope\"}")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/users/" + otherUser + "/roles").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"roles\":[\"USER\"]}")).andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void protectsTheLastEffectiveAdminAndAllowsChangesWithAnotherAdmin() throws Exception {
        secondAdmin = UUID.fromString(json(create("admin2@example.com", "Admin 2", "password123", "ADMIN"), "id"));
        mockMvc.perform(patch("/api/v1/users/" + secondAdmin).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"status\":\"DISABLED\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/" + secondAdmin).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"status\":\"ACTIVE\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/" + secondAdmin + "/roles").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"roles\":[\"USER\"]}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/" + UUID.fromString(jdbc.queryForObject("SELECT id FROM users WHERE email = 'admin@example.com'", String.class))).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"status\":\"DISABLED\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMIN_REQUIRED"));
        mockMvc.perform(put("/api/v1/users/" + UUID.fromString(jdbc.queryForObject("SELECT id FROM users WHERE email = 'admin@example.com'", String.class)) + "/roles").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"roles\":[\"USER\"]}")).andExpect(status().isConflict());
    }

    @Test
    @Order(8)
    void disabledUsersAndMembershipsDoNotCountAsEffectiveAdmins() throws Exception {
        UUID disabledUser = UUID.fromString(json(create("disabled-admin@example.com", "Disabled admin", "password123", "ADMIN"), "id"));
        mockMvc.perform(patch("/api/v1/users/" + disabledUser).header("Authorization", bearer(adminToken))
                        .contentType("application/json").content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        UUID adminId = UUID.fromString(jdbc.queryForObject("SELECT id FROM users WHERE email = 'admin@example.com'", String.class));
        mockMvc.perform(put("/api/v1/users/" + adminId + "/roles").header("Authorization", bearer(adminToken))
                        .contentType("application/json").content("{\"roles\":[\"USER\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMIN_REQUIRED"));

        mockMvc.perform(patch("/api/v1/users/" + disabledUser).header("Authorization", bearer(adminToken))
                        .contentType("application/json").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        jdbc.update("UPDATE organization_members SET status = 'DISABLED' WHERE user_id = ?", disabledUser.toString());
        mockMvc.perform(put("/api/v1/users/" + adminId + "/roles").header("Authorization", bearer(adminToken))
                        .contentType("application/json").content("{\"roles\":[\"USER\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMIN_REQUIRED"));
    }

    @Test
    @Order(9)
    void disabledUserCannotUseAnOldToken() throws Exception {
        UUID userId = UUID.fromString(json(create("disabled@example.com", "Disabled", "password123", "USER"), "id"));
        String token = login("disabled@example.com", "password123");
        mockMvc.perform(patch("/api/v1/users/" + userId).header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"status\":\"DISABLED\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(token))).andExpect(status().isForbidden());
    }

    private MvcResult create(String email, String displayName, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"email\":\"" + email + "\",\"displayName\":\"" + displayName + "\",\"password\":\"" + password + "\",\"roles\":[\"" + role + "\"]}"))
                .andExpect(status().isCreated()).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions createExpecting(String email, String... roles) throws Exception {
        String roleJson = String.join(",", java.util.Arrays.stream(roles).map(role -> "\"" + role + "\"").toList());
        return mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"email\":\"" + email + "\",\"displayName\":\"User\",\"password\":\"password123\",\"roles\":[" + roleJson + "]}"));
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.replaceFirst(".*\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private String json(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path(field).asText();
    }
}
