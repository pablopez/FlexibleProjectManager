package com.flexibleprojectmanager.platform.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuditInactiveLicenseIntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-audit-inactive-" + UUID.randomUUID(), "test.sqlite");
    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) { registry.add("app.database.path", () -> database.toString()); }

    @Test void inactiveLicenseBlocksAuditRead() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(
                "{\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = login.getResponse().getContentAsString().replaceFirst(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/v1/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
    }
}
