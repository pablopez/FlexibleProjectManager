package com.flexibleprojectmanager.platform.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.flexibleprojectmanager.platform.audit.application.AuditQuery;
import com.flexibleprojectmanager.platform.audit.application.AuditQueryUseCase;
import com.flexibleprojectmanager.platform.audit.application.AuditRecorder;
import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;
import com.flexibleprojectmanager.platform.audit.domain.AuditMetadata;
import com.flexibleprojectmanager.platform.licensing.LicensingTestConfiguration;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LicensingTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditIntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-audit-" + UUID.randomUUID(), "test.sqlite");
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuditRecorder recorder;
    @Autowired AuditQueryUseCase queries;
    private String token;
    private UUID organizationId;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) { registry.add("app.database.path", () -> database.toString()); }

    @BeforeAll void initialize() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(
                "{\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        token = login.getResponse().getContentAsString().replaceFirst(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        organizationId = UUID.fromString(jdbc.queryForObject("SELECT id FROM organizations LIMIT 1", String.class));
    }

    @Test void appendQueryRoundTripAndOrganizationFilters() {
        UUID actor = UUID.randomUUID(); UUID resource = UUID.randomUUID();
        recorder.record(new AuditEvent(UUID.randomUUID(), organizationId, actor, "TEST_SCOPED", "PROJECT", resource,
                new AuditMetadata(List.of("field,with,commas", "quoted\"field")), Instant.parse("2026-01-01T00:00:00Z")));
        UUID otherOrg = UUID.randomUUID();
        recorder.record(new AuditEvent(UUID.randomUUID(), otherOrg, actor, "TEST_SCOPED", "PROJECT", resource, null, Instant.parse("2026-01-02T00:00:00Z")));
        var page = queries.list(new CurrentActor(actor, organizationId, null, List.of("audit:read")),
                new AuditQuery(null, "TEST_SCOPED", "PROJECT", null, null, null, 0, 25));
        assertEquals(1, page.total());
        assertEquals(List.of("field,with,commas", "quoted\"field"), page.items().getFirst().changedFields());
        assertTrue(queries.list(new CurrentActor(actor, organizationId, null, List.of("audit:read")), new AuditQuery(null, null, null, null, null, null, 0, 25)).items().stream().noneMatch(item -> item.createdAt().equals(Instant.parse("2026-01-02T00:00:00Z"))));
    }

    @Test void actorNamesResolveThroughTheBatchLookup() {
        UUID adminId = UUID.fromString(jdbc.queryForObject("SELECT id FROM users WHERE email = 'admin@example.com'", String.class));
        UUID missingId = UUID.randomUUID();
        recorder.record(new AuditEvent(UUID.randomUUID(), organizationId, adminId, "BATCH_RESOLUTION", "USER", null, null, Instant.now()));
        recorder.record(new AuditEvent(UUID.randomUUID(), organizationId, missingId, "BATCH_RESOLUTION", "USER", null, null, Instant.now()));
        var page = queries.list(new CurrentActor(adminId, organizationId, null, List.of("audit:read")), new AuditQuery(null, "BATCH_RESOLUTION", "USER", null, null, null, 0, 25));
        assertEquals(2, page.total());
        assertTrue(page.items().stream().anyMatch(item -> item.actor() != null && item.actor().displayName().equals("Admin")));
        assertTrue(page.items().stream().anyMatch(item -> item.actor() == null));
    }

    @Test void actorWithoutAuditPermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(
                                "{\"email\":\"audit-no-read@example.com\",\"displayName\":\"No Audit Read\",\"password\":\"password123\",\"roles\":[\"USER\"]}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(
                "{\"email\":\"audit-no-read@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        String userToken = login.getResponse().getContentAsString().replaceFirst(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/v1/audit").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test void projectEventsAndNoOpUpdateBehavior() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + token).contentType("application/json").content("{\"name\":\"Audit project\",\"description\":\"D\"}"))
                .andExpect(status().isCreated()).andReturn();
        String id = created.getResponse().getContentAsString().replaceFirst(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        int before = jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE resource_id = ?", Integer.class, id);
        mockMvc.perform(patch("/api/v1/projects/" + id).header("Authorization", "Bearer " + token).contentType("application/json").content("{\"name\":\"Audit project\",\"description\":\"D\"}"))
                .andExpect(status().isOk());
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE resource_id = ?", Integer.class, id));
        mockMvc.perform(patch("/api/v1/projects/" + id).header("Authorization", "Bearer " + token).contentType("application/json").content("{\"name\":\"Changed\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects/" + id + "/archive").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects/" + id + "/restore").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit?action=PROJECT_CREATED").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].action").value("PROJECT_CREATED"));
        mockMvc.perform(get("/api/v1/audit?userId=not-a-uuid").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/audit?resourceId=not-a-uuid").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/audit?from=not-a-date").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/audit?from=2026-02-01T00:00:00Z&to=2026-01-01T00:00:00Z").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/audit?action=bad-action").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/audit?resourceType=bad-type").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/audit?size=201").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test void userAndSettingsEventsUseSemanticChanges() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token).contentType("application/json").content("{\"email\":\"audit-user@example.com\",\"displayName\":\"Audit User\",\"password\":\"password123\",\"roles\":[\"USER\"]}"))
                .andExpect(status().isCreated()).andReturn();
        String id = created.getResponse().getContentAsString().replaceFirst(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        mockMvc.perform(patch("/api/v1/users/" + id).header("Authorization", "Bearer " + token).contentType("application/json").content("{\"displayName\":\"Renamed\"}")) .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/" + id).header("Authorization", "Bearer " + token).contentType("application/json").content("{\"status\":\"DISABLED\"}")) .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/" + id).header("Authorization", "Bearer " + token).contentType("application/json").content("{\"status\":\"ACTIVE\"}")) .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/" + id + "/roles").header("Authorization", "Bearer " + token).contentType("application/json").content("{\"roles\":[\"VIEWER\"]}")) .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", "Bearer " + token).contentType("application/json").content("{\"language\":\"es\"}")) .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'USER_CREATED' AND resource_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'USER_UPDATED' AND resource_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'USER_DISABLED' AND resource_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'USER_REACTIVATED' AND resource_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'USER_ROLES_CHANGED' AND resource_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'USER_PREFERENCES_UPDATED'", Integer.class));
        var persistedMetadata = jdbc.queryForList("SELECT COALESCE(metadata, '') FROM audit_events", String.class);
        assertTrue(persistedMetadata.stream().noneMatch(value -> value.contains("password123")
                || value.contains("audit-user@example.com") || value.contains("admin@example.com")
                || value.contains("accessToken") || value.contains("refreshToken") || value.contains("BEGIN")));
    }
}
