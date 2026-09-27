package com.flexibleprojectmanager.platform.authentication.infrastructure;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PersistentRsaKeyProvider {
    private final Path keyPath;

    public PersistentRsaKeyProvider(@Value("${app.security.jwt-key-path}") String keyPath) {
        this.keyPath = Path.of(keyPath);
    }

    public synchronized KeyPair loadOrCreate() {
        try {
            if (Files.exists(keyPath)) {
                return read(Files.readString(keyPath));
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            Files.createDirectories(keyPath.toAbsolutePath().getParent());
            Files.writeString(keyPath, serialize(keyPair), StandardCharsets.US_ASCII,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return keyPair;
        } catch (FileAlreadyExistsException race) {
            try {
                return read(Files.readString(keyPath));
            } catch (Exception exception) {
                throw new IllegalStateException("JWT signing key could not be loaded.", exception);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("JWT signing key could not be loaded.", exception);
        }
    }

    private String serialize(KeyPair keyPair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n"
                + "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
    }

    private KeyPair read(String pem) throws Exception {
        byte[] privateBytes = decodePem(pem, "PRIVATE KEY");
        byte[] publicBytes = decodePem(pem, "PUBLIC KEY");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
        PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(publicBytes));
        return new KeyPair(publicKey, privateKey);
    }

    private byte[] decodePem(String pem, String label) {
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        int start = pem.indexOf(begin);
        int finish = pem.indexOf(end);
        if (start < 0 || finish < 0) {
            throw new IllegalArgumentException("Invalid JWT key material.");
        }
        return Base64.getMimeDecoder().decode(pem.substring(start + begin.length(), finish));
    }
}
