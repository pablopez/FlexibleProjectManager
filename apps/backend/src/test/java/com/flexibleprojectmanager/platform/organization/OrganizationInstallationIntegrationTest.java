package com.flexibleprojectmanager.platform.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;

import com.flexibleprojectmanager.platform.licensing.LicensingTestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LicensingTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class OrganizationInstallationIntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-organization-slice6-" + UUID.randomUUID(), "test.sqlite");
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    private String adminToken;
    private UUID organizationId;
    private UUID installationId;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) { registry.add("app.database.path", () -> database.toString()); }

    @BeforeAll
    void initialize() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        adminToken = login("admin@example.com", "password123");
    }

    @Test @Order(1)
    void administratorCanReadAndUpdateCurrentOrganizationAndInstallation() throws Exception {
        MvcResult org = mockMvc.perform(get("/api/v1/organization").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Org")).andReturn();
        organizationId = UUID.fromString(json(org, "id"));
        MvcResult installation = mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizationId").value(organizationId.toString())).andReturn();
        installationId = UUID.fromString(json(installation, "id"));

        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"name\":\"Updated Org\",\"slug\":\"updated-org\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(organizationId.toString())).andExpect(jsonPath("$.name").value("Updated Org"));
        mockMvc.perform(patch("/api/v1/installation").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"name\":\"Updated Local\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(installationId.toString())).andExpect(jsonPath("$.name").value("Updated Local"));
    }

    @Test @Order(2)
    void rejectsInvalidUpdatesAndIgnoresServerOwnedFields() throws Exception {
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"name\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"name\":\"Still Org\",\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"DISABLED\",\"installationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(organizationId.toString())).andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(patch("/api/v1/installation").header("Authorization", bearer(adminToken)).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/installation").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"name\":\"Still Local\",\"id\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + UUID.randomUUID() + "\",\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(installationId.toString())).andExpect(jsonPath("$.organizationId").value(organizationId.toString())).andExpect(jsonPath("$.status").value("UNLICENSED"));
    }

    @Test @Order(3)
    void preservesAndNormalizesOrganizationSlugAccordingToContract() throws Exception {
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"name\":\"Name only\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("updated-org"));
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"slug\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"slug\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value(""));
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"slug\":\" abc \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("abc"));
    }

    @Test @Order(3)
    void userCanReadButCannotUpdate() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"email\":\"reader@example.com\",\"displayName\":\"Reader\",\"password\":\"password123\",\"roles\":[\"USER\"]}"))
                .andExpect(status().isCreated()).andReturn();
        String userToken = login("reader@example.com", "password123");
        mockMvc.perform(get("/api/v1/organization").header("Authorization", bearer(userToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(userToken))).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(userToken)).contentType("application/json").content("{\"name\":\"Nope\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/installation").header("Authorization", bearer(userToken)).contentType("application/json").content("{\"name\":\"Nope\"}"))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertNotNull(json(created, "id"));
    }

    @Test @Order(4)
    void organizationUpdateDoesNotDeletePlatformData() throws Exception {
        UUID projectId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/projects").header("Authorization", bearer(adminToken)).contentType("application/json")
                        .content("{\"name\":\"Preserved project\",\"description\":\"Project data\"}"))
                .andExpect(status().isCreated());
        projectId = UUID.fromString(jdbc.queryForObject("SELECT id FROM projects WHERE name = 'Preserved project'", String.class));
        mockMvc.perform(patch("/api/v1/organization").header("Authorization", bearer(adminToken)).contentType("application/json").content("{\"name\":\"Preserved Org\"}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM organization_members", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM organizations", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM installations", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM projects WHERE id = ? AND organization_id = ? AND name = ? AND description = ? AND status = 'ACTIVE'", Integer.class,
                projectId.toString(), organizationId.toString(), "Preserved project", "Project data"));
    }

    @Test @Order(5)
    void installationFromAnotherOrganizationIsNotExposed() throws Exception {
        UUID otherOrganizationId = UUID.randomUUID();
        UUID otherInstallationId = UUID.randomUUID();
        String now = Instant.now().toString();
        jdbc.update("INSERT INTO organizations (id, name, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                otherOrganizationId.toString(), "Other Org", "ACTIVE", now, now);
        jdbc.update("INSERT INTO installations (id, organization_id, name, platform, application_version, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                otherInstallationId.toString(), otherOrganizationId.toString(), "Other Local", "OTHER", "0.1.0", "ACTIVE", now);

        mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(installationId.toString()))
                .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
                .andExpect(jsonPath("$.name").value("Still Local"));
    }

    @Test @Order(6)
    void multipleInstallationsFailDeterministicallyInsteadOfSelectingOne() throws Exception {
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO installations (id, organization_id, name, platform, application_version, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                other.toString(), organizationId.toString(), "Unexpected", "OTHER", "0.1.0", "ACTIVE", Instant.now().toString());
        mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(adminToken)))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INSTALLATION_INVARIANT_BROKEN"));
    }

    @Test @Order(7)
    void noInstallationReturnsNotFoundInsteadOfCreatingOne() throws Exception {
        jdbc.update("DELETE FROM installations");
        mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INSTALLATION_NOT_FOUND"));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json(result, "accessToken");
    }
    private String json(MvcResult result, String field) throws Exception { return objectMapper.readTree(result.getResponse().getContentAsString()).path(field).asText(); }
    private static String bearer(String token) { return "Bearer " + token; }
}
