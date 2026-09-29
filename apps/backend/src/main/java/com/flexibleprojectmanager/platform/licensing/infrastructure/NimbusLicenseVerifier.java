package com.flexibleprojectmanager.platform.licensing.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSObject;
import com.flexibleprojectmanager.platform.licensing.application.LicenseException;
import com.flexibleprojectmanager.platform.licensing.application.LicenseVerifier;
import com.flexibleprojectmanager.platform.licensing.domain.LicenseType;
import com.flexibleprojectmanager.platform.licensing.domain.VerifiedLicense;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

@Component
public class NimbusLicenseVerifier implements LicenseVerifier {
    private static final Pattern FEATURE = Pattern.compile("^[a-z][a-z0-9.-]*$");
    private final LicenseProperties properties;
    private final ObjectMapper objectMapper;

    public NimbusLicenseVerifier(LicenseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public VerifiedLicense verify(String signedLicense) {
        final JWSObject jws;
        try {
            if (signedLicense == null || signedLicense.isBlank()) throw new LicenseException("LICENSE_INVALID");
            jws = JWSObject.parse(signedLicense);
        } catch (LicenseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LicenseException("LICENSE_SIGNATURE_INVALID");
        }

        if (!"EdDSA".equals(jws.getHeader().getAlgorithm().getName())) {
            throw new LicenseException("LICENSE_SIGNATURE_INVALID");
        }
        String kid = jws.getHeader().getKeyID();
        if (kid == null || kid.isBlank() || !properties.getTrustedKeys().containsKey(kid)) {
            throw new LicenseException("LICENSE_KEY_UNKNOWN");
        }

        try {
            PublicKey publicKey = publicKey(properties.getTrustedKeys().get(kid));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(jws.getSigningInput());
            if (!verifier.verify(jws.getSignature().decode())) {
                throw new LicenseException("LICENSE_SIGNATURE_INVALID");
            }
        } catch (LicenseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LicenseException("LICENSE_SIGNATURE_INVALID");
        }

        return parseTrustedPayload(jws.getPayload().toString());
    }

    private VerifiedLicense parseTrustedPayload(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root == null || !root.isObject()) throw new LicenseException("LICENSE_INVALID");
            Set<String> allowed = Set.of("version", "licenseId", "installationId", "type", "issuedAt",
                    "expiresAt", "maxUsers", "licenseFeatures");
            for (var field : root.properties()) {
                if (!allowed.contains(field.getKey())) throw new LicenseException("LICENSE_INVALID");
            }

            JsonNode version = required(root, "version");
            if (!version.isIntegralNumber() || version.intValue() != 1) throw new LicenseException("LICENSE_VERSION_UNSUPPORTED");
            UUID licenseId = uuid(required(root, "licenseId"));
            UUID installationId = uuid(required(root, "installationId"));
            LicenseType type = enumValue(required(root, "type"), LicenseType.class);
            Instant issuedAt = instant(required(root, "issuedAt"));
            JsonNode expiryNode = root.get("expiresAt");
            Instant expiresAt = expiryNode == null || expiryNode.isNull() ? null : instant(expiryNode);
            JsonNode maxUsers = required(root, "maxUsers");
            if (!maxUsers.isIntegralNumber() || !maxUsers.canConvertToInt() || maxUsers.intValue() < 1) {
                throw new LicenseException("LICENSE_INVALID");
            }
            JsonNode featureNode = required(root, "licenseFeatures");
            if (!featureNode.isArray()) throw new LicenseException("LICENSE_INVALID");
            var features = new ArrayList<String>();
            for (JsonNode feature : featureNode) {
                if (!feature.isTextual() || !FEATURE.matcher(feature.textValue()).matches()) {
                    throw new LicenseException("LICENSE_INVALID");
                }
                features.add(feature.textValue());
            }
            if (features.size() != new HashSet<>(features).size()) throw new LicenseException("LICENSE_INVALID");
            if ((type == LicenseType.PERPETUAL) != (expiresAt == null)) throw new LicenseException("LICENSE_INVALID");
            if (type != LicenseType.PERPETUAL && expiresAt == null) throw new LicenseException("LICENSE_INVALID");
            if (expiresAt != null && !expiresAt.isAfter(issuedAt)) throw new LicenseException("LICENSE_INVALID");
            return new VerifiedLicense(1, licenseId, installationId, type, issuedAt, expiresAt, maxUsers.intValue(), features, "Ed25519");
        } catch (LicenseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LicenseException("LICENSE_INVALID");
        }
    }

    private PublicKey publicKey(String encoded) throws Exception {
        byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private JsonNode required(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || value.isNull()) throw new LicenseException("LICENSE_INVALID");
        return value;
    }

    private UUID uuid(JsonNode value) {
        try { return UUID.fromString(value.textValue()); }
        catch (Exception exception) { throw new LicenseException("LICENSE_INVALID"); }
    }

    private Instant instant(JsonNode value) {
        try { return Instant.parse(value.textValue()); }
        catch (Exception exception) { throw new LicenseException("LICENSE_INVALID"); }
    }

    private <T extends Enum<T>> T enumValue(JsonNode value, Class<T> type) {
        try { return Enum.valueOf(type, value.textValue()); }
        catch (Exception exception) { throw new LicenseException("LICENSE_INVALID"); }
    }
}
