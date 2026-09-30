package com.flexibleprojectmanager.platform.audit.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.audit.application.AuditQueryUseCase;

public final class AuditDtos {
    private AuditDtos() {}
    public record Actor(UUID id, String displayName) {}
    public record Entry(UUID id, Actor actor, String action, String resourceType, UUID resourceId, Metadata metadata, Instant createdAt) {
        static Entry from(AuditQueryUseCase.Entry entry) { return new Entry(entry.id(), entry.actor() == null ? null : new Actor(entry.actor().id(), entry.actor().displayName()), entry.action(), entry.resourceType(), entry.resourceId(), entry.changedFields() == null ? null : new Metadata(entry.changedFields()), entry.createdAt()); }
    }
    public record Metadata(List<String> changedFields) {}
    public record ListResponse(List<Entry> items, int page, int size, long total, int totalPages) {
        static ListResponse from(AuditQueryUseCase.PageResult result) { return new ListResponse(result.items().stream().map(Entry::from).toList(), result.page(), result.size(), result.total(), result.totalPages()); }
    }
}
