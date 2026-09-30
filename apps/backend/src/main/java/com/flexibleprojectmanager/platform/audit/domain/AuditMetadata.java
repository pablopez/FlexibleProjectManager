package com.flexibleprojectmanager.platform.audit.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record AuditMetadata(List<String> changedFields) {
    public AuditMetadata {
        if (changedFields == null || changedFields.isEmpty() || changedFields.size() > 32) {
            throw new IllegalArgumentException("Audit changed fields must contain between 1 and 32 values.");
        }
        List<String> values = List.copyOf(new LinkedHashSet<>(changedFields));
        if (values.size() != changedFields.size() || values.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 64)) {
            throw new IllegalArgumentException("Audit changed fields must be unique non-blank values of at most 64 characters.");
        }
        changedFields = values;
    }

    public static AuditMetadata of(String... fields) {
        return new AuditMetadata(List.of(fields));
    }
}
