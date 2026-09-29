package com.flexibleprojectmanager.platform.licensing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class LicenseIntegrationTest {
    private static final Path database = Path.of(System.getProperty("java.io.tmpdir"), "fpm-license-slice7-" + UUID.randomUUID(), "test.sqlite");
    private static final KeyPair keyPair = keyPair();

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    private String token;
    private String activeSignedLicense;
    private UUID installationId;
    private UUID secondUserId;
    private UUID thirdUserId;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", () -> database.toString());
        registry.add("app.licensing.trusted-keys.test-key", () -> Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }

    @BeforeAll
    void initialize() throws Exception {
        mockMvc.perform(post("/api/v1/setup/initialize").contentType("application/json").content(
                "{\"organization\":{\"name\":\"Org\"},\"installation\":{\"name\":\"Local\"},\"administrator\":{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"password123\"}}"))
                .andExpect(status().isCreated());
        token = login();
        MvcResult installation = mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        installationId = UUID.fromString(objectMapper.readTree(installation.getResponse().getContentAsString()).path("id").asText());
    }

    @Test @Order(1)
    void activatesLicenseAndAllowsNormalBusinessOperations() throws Exception {
        activeSignedLicense = jsonLicense(installationId, "SUBSCRIPTION", "2099-01-01T00:00:00Z", 2, "[]");
        mockMvc.perform(post("/api/v1/license/activate").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"signedLicense\":\"" + activeSignedLicense + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.installationId").value(installationId.toString()))
                .andExpect(jsonPath("$.organizationId").exists()).andExpect(jsonPath("$.limits.maxUsers").value(2));
        mockMvc.perform(get("/api/v1/license/entitlements").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxUsers").value(2))
                .andExpect(jsonPath("$.licenseFeatures").isEmpty());
        mockMvc.perform(get("/api/v1/settings/user").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.theme").value("light"));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_preferences", Integer.class));
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"theme\":\"dark\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.theme").value("dark"));
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"language\":\"es\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.language").value("es"))
                .andExpect(jsonPath("$.theme").value("dark"));
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"language\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"theme\":null}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/settings/user").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"unknown\":\"value\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/settings/user").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.language").value("es"))
                .andExpect(jsonPath("$.theme").value("dark"));
        mockMvc.perform(post("/api/v1/projects").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"name\":\"Licensed project\"}"))
                .andExpect(status().isCreated());
        MvcResult secondUser = mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"email\":\"second@example.com\",\"displayName\":\"Second\",\"password\":\"password123\",\"roles\":[\"USER\"]}"))
                .andExpect(status().isCreated()).andReturn();
        secondUserId = UUID.fromString(objectMapper.readTree(secondUser.getResponse().getContentAsString()).path("id").asText());
        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"email\":\"third@example.com\",\"displayName\":\"Third\",\"password\":\"password123\",\"roles\":[\"USER\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LICENSE_USER_LIMIT_EXCEEDED"));
        mockMvc.perform(patch("/api/v1/users/" + secondUserId).header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        MvcResult thirdUser = mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"email\":\"third@example.com\",\"displayName\":\"Third\",\"password\":\"password123\",\"roles\":[\"USER\"]}"))
                .andExpect(status().isCreated()).andReturn();
        thirdUserId = UUID.fromString(objectMapper.readTree(thirdUser.getResponse().getContentAsString()).path("id").asText());
        mockMvc.perform(patch("/api/v1/users/" + secondUserId).header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LICENSE_USER_LIMIT_EXCEEDED"));
        mockMvc.perform(patch("/api/v1/users/" + thirdUserId).header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/" + secondUserId).header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test @Order(2)
    void futureIssuedPersistedLicenseIsInvalidAndBlocked() throws Exception {
        String future = jsonLicenseWithIssuedAt(installationId, "SUBSCRIPTION", "2100-01-01T00:00:00Z", 2, "[]", "2099-01-01T00:00:00Z");
        jdbc.update("UPDATE installation_licenses SET signed_license = ? WHERE installation_id = ?", future, installationId.toString());
        mockMvc.perform(get("/api/v1/license").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INVALID"));
        mockMvc.perform(get("/api/v1/license/entitlements").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxUsers").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.licenseFeatures").isEmpty());
        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/settings/user").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        jdbc.update("UPDATE installation_licenses SET signed_license = ? WHERE installation_id = ?", activeSignedLicense, installationId.toString());
    }

    @Test @Order(3)
    void expiredPersistedLicenseIsBlocked() throws Exception {
        String expired = jsonLicense(installationId, "SUBSCRIPTION", "2020-01-01T00:00:00Z", 1, "[]");
        jdbc.update("UPDATE installation_licenses SET signed_license = ? WHERE installation_id = ?", expired, installationId.toString());
        mockMvc.perform(get("/api/v1/license").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/settings/user").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        jdbc.update("UPDATE installation_licenses SET signed_license = ? WHERE installation_id = ?", activeSignedLicense, installationId.toString());
    }

    @Test @Order(4)
    void expiredCandidateDoesNotReplaceCurrentLicense() throws Exception {
        mockMvc.perform(post("/api/v1/license/activate").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"signedLicense\":\"" + jsonLicense(installationId, "SUBSCRIPTION", "2020-01-01T00:00:00Z", 1, "[]") + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LICENSE_EXPIRED"));
        mockMvc.perform(get("/api/v1/license").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(5)
    void deactivationLeavesDataAndBlocksNormalOperations() throws Exception {
        mockMvc.perform(post("/api/v1/license/deactivate").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/license").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("LICENSE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/license/entitlements").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxUsers").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.licenseFeatures").isEmpty());
        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM projects", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));

        MvcResult recoveryLogin = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        String recoveryToken = objectMapper.readTree(recoveryLogin.getResponse().getContentAsString()).path("accessToken").asText();
        Cookie refreshCookie = recoveryLogin.getResponse().getCookie("fpm_refresh_token");
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(recoveryToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/installation").header("Authorization", bearer(recoveryToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/license").header("Authorization", bearer(recoveryToken))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/license/entitlements").header("Authorization", bearer(recoveryToken))).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/installation").header("Authorization", bearer(recoveryToken))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(recoveryToken)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/organization").header("Authorization", bearer(recoveryToken)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        mockMvc.perform(get("/api/v1/settings/user").header("Authorization", bearer(recoveryToken)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        mockMvc.perform(post("/api/v1/license/activate").header("Authorization", bearer(recoveryToken)).contentType("application/json")
                        .content("{\"signedLicense\":\"" + activeSignedLicense + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(6)
    void corruptArtifactIsInvalidWithoutExposingPayload() throws Exception {
        jdbc.update("UPDATE installation_licenses SET signed_license = ? WHERE installation_id = ?",
                "not-a-license", installationId.toString());
        mockMvc.perform(get("/api/v1/license").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.licenseId").value(org.hamcrest.Matchers.nullValue())).andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.installationId").value(installationId.toString()));
        mockMvc.perform(get("/api/v1/license/entitlements").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxUsers").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.licenseFeatures").isEmpty());
        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LICENSE_NOT_ACTIVE"));
        jdbc.update("DELETE FROM installation_licenses WHERE installation_id = ?", installationId.toString());
        mockMvc.perform(post("/api/v1/license/deactivate").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("LICENSE_NOT_FOUND"));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private static String jsonLicense(UUID installationId, String type, String expiresAt, int maxUsers, String features) throws Exception {
        return jsonLicenseWithIssuedAt(installationId, type, expiresAt, maxUsers, features, "2019-01-01T00:00:00Z");
    }

    private static String jsonLicenseWithIssuedAt(UUID installationId, String type, String expiresAt, int maxUsers,
                                                  String features, String issuedAt) throws Exception {
        String payload = "{\"version\":1,\"licenseId\":\"" + UUID.randomUUID() + "\",\"installationId\":\"" + installationId
                + "\",\"type\":\"" + type + "\",\"issuedAt\":\"" + issuedAt + "\",\"expiresAt\":\"" + expiresAt
                + "\",\"maxUsers\":" + maxUsers + ",\"licenseFeatures\":" + features + "}";
        String header = "{\"alg\":\"EdDSA\",\"kid\":\"test-key\"}";
        String input = encode(header) + "." + encode(payload);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(keyPair.getPrivate());
        signature.update(input.getBytes(StandardCharsets.US_ASCII));
        return input + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    private static KeyPair keyPair() {
        try { return KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); }
        catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    private static String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String bearer(String value) { return "Bearer " + value; }
}
