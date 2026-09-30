package com.flexibleprojectmanager.platform.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.ServletException;

import com.flexibleprojectmanager.platform.audit.application.AuditRecorder;
import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;
import com.flexibleprojectmanager.platform.licensing.LicensingTestConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({LicensingTestConfiguration.class, AuditRollbackIntegrationTest.ThrowingAuditConfiguration.class})
class AuditRollbackIntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-audit-rollback-" + UUID.randomUUID(), "test.sqlite");
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) { registry.add("app.database.path", () -> database.toString()); }

    @Test void auditFailureRollsBackBusinessMutationAndAuditInsert() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content(
                "{\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = login.getResponse().getContentAsString().replaceFirst(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        assertThrows(ServletException.class, () -> mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"name\":\"Must roll back\",\"description\":null}")).andReturn());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM projects WHERE name = 'Must roll back'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action = 'PROJECT_CREATED'", Integer.class));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ThrowingAuditConfiguration {
        @Bean @Primary AuditRecorder throwingAuditRecorder() {
            return (AuditEvent event) -> { throw new IllegalStateException("audit persistence failed"); };
        }
    }
}
