package com.flexibleprojectmanager.platform.audit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.audit.application.AuditQuery;
import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;
import com.flexibleprojectmanager.platform.audit.domain.AuditMetadata;

class AuditDomainTest {
    @Test void metadataIsBoundedAndUnique() {
        assertEquals(List.of("name", "status"), new AuditMetadata(List.of("name", "status")).changedFields());
        assertThrows(IllegalArgumentException.class, () -> new AuditMetadata(List.of("name", "name")));
        assertThrows(IllegalArgumentException.class, () -> new AuditMetadata(List.of("x".repeat(65))));
    }

    @Test void eventCodesAreExtensibleButStrictlyMachineReadable() {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "FUTURE_ACTION", "FUTURE_RESOURCE", null, null, Instant.now());
        assertEquals("FUTURE_ACTION", event.action());
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(UUID.randomUUID(), UUID.randomUUID(), null, "bad-action", "USER", null, null, Instant.now()));
    }

    @Test void queryRejectsReversedTimeAndInvalidPagination() {
        assertThrows(IllegalArgumentException.class, () -> new AuditQuery(null, null, null, null, Instant.now(), Instant.now().minusSeconds(1), 0, 25));
        assertThrows(IllegalArgumentException.class, () -> new AuditQuery(null, null, null, null, null, null, 0, 201));
    }
}
