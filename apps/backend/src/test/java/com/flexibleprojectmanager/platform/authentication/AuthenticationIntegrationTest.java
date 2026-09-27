package com.flexibleprojectmanager.platform.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
class AuthenticationIntegrationTest {
    private static final Path databasePath = Path.of(
            System.getProperty("java.io.tmpdir"), "fpm-auth-" + UUID.randomUUID(), "test.sqlite");
    private static final Path keyPath = Path.of(
            System.getProperty("java.io.tmpdir"), "fpm-auth-" + UUID.randomUUID(), "jwt-keypair.pem");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", () -> databasePath.toString());
        registry.add("app.security.jwt-key-path", () -> keyPath.toString());
    }

    @Test
    @Order(1)
    void bootstrapsAnAdministrator() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize")
                        .contentType("application/json")
                        .content("{\"organization\":{\"name\":\"Example Studio\"},\"installation\":{\"name\":\"Workstation\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Administrator\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    void validLoginReturnsAccessTokenAndOnlyHashedRefreshToken() throws Exception {
        MvcResult result = login("admin@example.com", "password123");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("refreshToken");
        assertThat(result.getResponse().getCookie("fpm_refresh_token")).isNotNull();

        String rawToken = result.getResponse().getCookie("fpm_refresh_token").getValue();
        String storedHash = jdbcTemplate.queryForObject("SELECT token_hash FROM refresh_sessions", String.class);
        assertThat(storedHash).isNotEqualTo(rawToken);
        assertThat(storedHash).hasSize(64);
    }

    @Test
    @Order(3)
    void invalidPasswordAndUnknownEmailHaveTheSamePublicFailure() throws Exception {
        MvcResult wrongPassword = login("admin@example.com", "wrong-password");
        MvcResult unknownEmail = login("unknown@example.com", "wrong-password");

        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(401);
        assertThat(normalizeError(wrongPassword.getResponse().getContentAsString()))
                .doesNotContain("admin@example.com", "unknown@example.com")
                .isEqualToIgnoringWhitespace(normalizeError(unknownEmail.getResponse().getContentAsString()));
    }

    @Test
    @Order(4)
    void bearerTokenProtectsMeAndTamperingFails() throws Exception {
        MvcResult login = login("admin@example.com", "password123");
        String accessToken = json(login, "accessToken");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.displayName").value("Administrator"))
                .andExpect(jsonPath("$.organization.name").value("Example Studio"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void expiredAccessTokenIsRejected() throws Exception {
        var now = java.time.Instant.now();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("fpm-local-rs256").build(),
                JwtClaimsSet.builder()
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(now.minusSeconds(120))
                        .expiresAt(now.minusSeconds(1))
                        .build())).getTokenValue();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void concurrentRefreshAllowsOnlyOneRotationAndOldTokenCannotBeReused() throws Exception {
        MvcResult login = login("admin@example.com", "password123");
        Cookie original = login.getResponse().getCookie("fpm_refresh_token");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = executor.submit(() -> refresh(original));
            Future<MvcResult> second = executor.submit(() -> refresh(original));
            MvcResult firstResult = first.get();
            MvcResult secondResult = second.get();
            int successful = (firstResult.getResponse().getStatus() == 200 ? 1 : 0)
                    + (secondResult.getResponse().getStatus() == 200 ? 1 : 0);
            int failed = (firstResult.getResponse().getStatus() == 401 ? 1 : 0)
                    + (secondResult.getResponse().getStatus() == 401 ? 1 : 0);
            assertThat(successful).isEqualTo(1);
            assertThat(failed).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(refresh(original).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @Order(7)
    void logoutRevokesRefreshSessionAndExpiresCookie() throws Exception {
        MvcResult login = login("admin@example.com", "password123");
        Cookie cookie = login.getResponse().getCookie("fpm_refresh_token");
        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie)).andReturn();
        assertThat(logout.getResponse().getStatus()).isEqualTo(204);
        assertThat(logout.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @Order(8)
    void disabledUserCannotLogin() throws Exception {
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED'");
        assertThat(login("admin@example.com", "password123").getResponse().getStatus()).isEqualTo(401);
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    private MvcResult refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andReturn();
    }

    private String normalizeError(String body) {
        return body.replaceAll("\\\"timestamp\\\":\\\"[^\\\"]+\\\"", "\"timestamp\":\"same\"");
    }

    @SuppressWarnings("unchecked")
    private String json(MvcResult result, String property) throws Exception {
        Map<String, Object> body = (Map<String, Object>) objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        return (String) body.get(property);
    }
}
