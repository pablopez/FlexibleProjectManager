package com.flexibleprojectmanager.platform.licensing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.licensing.application.LicenseException;
import com.flexibleprojectmanager.platform.licensing.infrastructure.LicenseProperties;
import com.flexibleprojectmanager.platform.licensing.infrastructure.NimbusLicenseVerifier;

import tools.jackson.databind.json.JsonMapper;

class LicenseVerifierTest {
    private KeyPair keyPair;
    private NimbusLicenseVerifier verifier;
    private UUID installationId;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        keyPair = generator.generateKeyPair();
        LicenseProperties properties = new LicenseProperties();
        properties.setTrustedKeys(Map.of("test-key", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())));
        verifier = new NimbusLicenseVerifier(properties, new JsonMapper());
        installationId = UUID.randomUUID();
    }

    @Test
    void verifiesValidEd25519LicenseAndClaims() throws Exception {
        var license = verifier.verify(sign(payload(installationId, "SUBSCRIPTION", "\"2027-01-01T00:00:00Z\"", "25", "[\"module.video-qc\"]")));
        assertEquals(installationId, license.installationId());
        assertEquals(25, license.maxUsers());
        assertEquals("module.video-qc", license.licenseFeatures().get(0));
    }

    @Test
    void rejectsAlteredPayloadAndUnknownKey() throws Exception {
        String signed = sign(payload(installationId, "PERPETUAL", "null", "1", "[]"));
        String[] parts = signed.split("\\.");
        String altered = parts[0] + "." + encode((payload(installationId, "PERPETUAL", "null", "2", "[]"))) + "." + parts[2];
        assertEquals("LICENSE_SIGNATURE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(altered)).code());

        String alteredSignature = parts[0] + "." + parts[1] + "."
                + (parts[2].charAt(0) == 'A' ? 'B' : 'A') + parts[2].substring(1);
        assertEquals("LICENSE_SIGNATURE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(alteredSignature)).code());

        String unknownHeader = signWithHeader("unknown", payload(installationId, "PERPETUAL", "null", "1", "[]"));
        assertEquals("LICENSE_KEY_UNKNOWN", assertThrows(LicenseException.class, () -> verifier.verify(unknownHeader)).code());
    }

    @Test
    void rejectsWrongAlgorithmUnsupportedVersionAndInvalidFeatures() throws Exception {
        String wrongAlgorithm = signWithHeader("test-key", "Ed25519", payload(installationId, "PERPETUAL", "null", "1", "[]"));
        assertEquals("LICENSE_SIGNATURE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(wrongAlgorithm)).code());
        String unsupported = sign(payloadWithVersion(2, installationId));
        assertEquals("LICENSE_VERSION_UNSUPPORTED", assertThrows(LicenseException.class, () -> verifier.verify(unsupported)).code());
        String duplicate = sign(payload(installationId, "PERPETUAL", "null", "1", "[\"a\",\"a\"]"));
        assertEquals("LICENSE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(duplicate)).code());

        String malformed = "not.a.compact-jws";
        assertEquals("LICENSE_SIGNATURE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(malformed)).code());

        String invalidMaxUsers = sign(payload(installationId, "PERPETUAL", "null", "0", "[]"));
        assertEquals("LICENSE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(invalidMaxUsers)).code());

        String invalidFeature = sign(payload(installationId, "PERPETUAL", "null", "1", "[\"Module.Video\"]"));
        assertEquals("LICENSE_INVALID", assertThrows(LicenseException.class, () -> verifier.verify(invalidFeature)).code());
    }

    private String payload(UUID installation, String type, String expiresAt, String maxUsers, String features) {
        return "{\"version\":1,\"licenseId\":\"" + UUID.randomUUID() + "\",\"installationId\":\"" + installation
                + "\",\"type\":\"" + type + "\",\"issuedAt\":\"2026-01-01T00:00:00Z\",\"expiresAt\":" + expiresAt
                + ",\"maxUsers\":" + maxUsers + ",\"licenseFeatures\":" + features + "}";
    }

    private String payloadWithVersion(int version, UUID installation) {
        return payload(installation, "PERPETUAL", "null", "1", "[]").replace("\"version\":1", "\"version\":" + version);
    }

    private String sign(String payload) throws Exception { return signWithHeader("test-key", payload); }

    private String signWithHeader(String kid, String payload) throws Exception { return signWithHeader(kid, "EdDSA", payload); }

    private String signWithHeader(String kid, String algorithm, String payload) throws Exception {
        String header = "{\"alg\":\"" + algorithm + "\",\"kid\":\"" + kid + "\"}";
        String input = encode(header) + "." + encode(payload);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(keyPair.getPrivate());
        signature.update(input.getBytes(StandardCharsets.US_ASCII));
        return input + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
