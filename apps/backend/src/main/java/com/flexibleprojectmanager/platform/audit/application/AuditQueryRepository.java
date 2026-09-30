package com.flexibleprojectmanager.platform.audit.application;

import java.util.List;
import java.util.UUID;

public interface AuditQueryRepository {
    PageResult find(UUID organizationId, AuditQuery query);
    record PageResult(List<AuditQueryUseCase.Entry> items, long total) {}
}
