package com.flexibleprojectmanager.platform.projects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;

import com.flexibleprojectmanager.platform.licensing.LicensingTestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LicensingTestConfiguration.class)
class ProjectIntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-projects-" + UUID.randomUUID(), "test.sqlite");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) { registry.add("app.database.path", () -> database.toString()); }

    @Test
    void createsListsAndArchivesGenericProjectThroughApplicationBoundary() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(
                "{\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = login.getResponse().getContentAsString().replaceFirst(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        MvcResult created = mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"Generic project\",\"description\":\"Platform-owned data only\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.name").value("Generic project")).andReturn();
        String projectId = created.getResponse().getContentAsString().replaceFirst(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(25)).andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Platform-owned data only"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").doesNotExist());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + token).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/archive").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(get("/api/v1/projects?status=ARCHIVED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));

        jdbc.update("UPDATE users SET status = 'DISABLED'");
        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET status = 'ACTIVE'");
        jdbc.update("UPDATE organization_members SET status = 'DISABLED'");
        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE organization_members SET status = 'ACTIVE'");
        jdbc.update("UPDATE organizations SET status = 'DISABLED'");
        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
