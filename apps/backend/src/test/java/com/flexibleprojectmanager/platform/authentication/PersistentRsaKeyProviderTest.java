package com.flexibleprojectmanager.platform.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.flexibleprojectmanager.platform.authentication.infrastructure.PersistentRsaKeyProvider;

class PersistentRsaKeyProviderTest {
    @TempDir Path tempDir;

    @Test
    void createsAndReusesKeyWithOwnerOnlyPermissionsWhenSupported() throws Exception {
        Path key = tempDir.resolve("security").resolve("jwt-keypair.pem");
        PersistentRsaKeyProvider provider = new PersistentRsaKeyProvider(key.toString());

        var first = provider.loadOrCreate();
        String pem = Files.readString(key);
        var second = provider.loadOrCreate();

        assertThat(second.getPrivate().getEncoded()).isEqualTo(first.getPrivate().getEncoded());
        assertThat(Files.readString(key)).isEqualTo(pem);
        if (key.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertThat(Files.getPosixFilePermissions(key)).containsExactlyInAnyOrder(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            assertThat(Files.getPosixFilePermissions(key.getParent())).containsExactlyInAnyOrder(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE);
        }
    }
}
