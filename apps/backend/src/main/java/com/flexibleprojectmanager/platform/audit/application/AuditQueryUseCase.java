package com.flexibleprojectmanager.platform.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface AuditQueryUseCase {
    PageResult list(CurrentActor actor, AuditQuery query);

    record PageResult(List<Entry> items, int page, int size, long total, int totalPages) {}
    record Entry(UUID id, Actor actor, String action, String resourceType, UUID resourceId,
                 List<String> changedFields, Instant createdAt) {}
    record Actor(UUID id, String displayName) {}
}
