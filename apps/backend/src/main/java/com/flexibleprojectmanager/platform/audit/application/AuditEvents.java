package com.flexibleprojectmanager.platform.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;
import com.flexibleprojectmanager.platform.audit.domain.AuditMetadata;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public final class AuditEvents {
    private AuditEvents() {}
    public static void record(AuditRecorder recorder, CurrentActor actor, String action, String resourceType, UUID resourceId, AuditMetadata metadata, Instant at) {
        recorder.record(new AuditEvent(UUID.randomUUID(), actor.organizationId(), actor.userId(), action, resourceType, resourceId, metadata, at));
    }
    public static AuditMetadata fields(List<String> fields) { return fields.isEmpty() ? null : new AuditMetadata(fields); }
}
