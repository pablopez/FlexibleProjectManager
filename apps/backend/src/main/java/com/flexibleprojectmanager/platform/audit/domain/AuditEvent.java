package com.flexibleprojectmanager.platform.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(UUID id, UUID organizationId, UUID actorUserId, String action,
                         String resourceType, UUID resourceId, AuditMetadata metadata, Instant occurredAt) {
    public AuditEvent {
        if (id == null || organizationId == null || occurredAt == null) throw new IllegalArgumentException("Audit identity and time are required.");
        validateCode(action, "action");
        validateCode(resourceType, "resourceType");
    }

    private static void validateCode(String value, String field) {
        if (value == null || value.length() > 64 || !value.matches("^[A-Z][A-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid audit " + field + ".");
        }
    }
}
