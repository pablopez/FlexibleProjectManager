package com.flexibleprojectmanager.platform.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgresql")
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fpm")
            .withUsername("fpm")
            .withPassword("fpm-test-password");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void appliesAllMigrationsAndInitializesRepresentativeData() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'user_preferences'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'audit_events'", Integer.class))
                .isEqualTo(1);

        mockMvc.perform(get("/api/v1/setup/status")).andExpect(status().isOk());
        MvcResult setupResult = mockMvc.perform(post("/api/v1/setup/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organization":{"name":"Postgres Org"},"installation":{"name":"Postgres"},"administrator":{"email":"admin@postgres.test","displayName":"Admin","password":"password123"}}
                                """))
                .andReturn();

        int setupStatus = setupResult.getResponse().getStatus();
        if (setupStatus != 201) {
            Throwable resolved = setupResult.getResolvedException();
            Throwable deepest = deepestCause(resolved);
            assertThat(setupStatus)
                    .withFailMessage("PostgreSQL setup returned %d; response=%s; resolvedException=%s; deepestCause=%s: %s",
                            setupStatus,
                            setupResult.getResponse().getContentAsString(),
                            resolved == null ? "none" : resolved.getClass().getName(),
                            deepest == null ? "none" : deepest.getClass().getName(),
                            deepest == null ? "none" : deepest.getMessage())
                    .isEqualTo(201);
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM organizations", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    private static Throwable deepestCause(Throwable exception) {
        Throwable deepest = exception;
        while (deepest != null && deepest.getCause() != null) {
            deepest = deepest.getCause();
        }
        return deepest;
    }
}
