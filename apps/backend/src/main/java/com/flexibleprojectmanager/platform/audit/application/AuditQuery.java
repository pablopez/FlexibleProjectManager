package com.flexibleprojectmanager.platform.audit.application;

import java.time.Instant;
import java.util.UUID;

public record AuditQuery(UUID userId, String action, String resourceType, UUID resourceId,
                         Instant from, Instant to, int page, int size) {
    public AuditQuery {
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("Invalid pagination.");
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("The from time must not be after the to time.");
        if (action != null) validateCode(action, "action");
        if (resourceType != null) validateCode(resourceType, "resourceType");
    }

    private static void validateCode(String value, String field) {
        if (value.length() > 64 || !value.matches("^[A-Z][A-Z0-9_]*$")) throw new IllegalArgumentException("Invalid audit " + field + ".");
    }
}
