package com.flexibleprojectmanager.platform.audit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

@Service
public class AuditQueryService implements AuditQueryUseCase {
    private final AuditQueryRepository repository;

    public AuditQueryService(AuditQueryRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public PageResult list(CurrentActor actor, AuditQuery query) {
        actor.requirePermission("audit:read");
        AuditQueryRepository.PageResult result = repository.find(actor.organizationId(), query);
        return new PageResult(result.items(), query.page(), query.size(), result.total(), (int) Math.ceil((double) result.total() / query.size()));
    }
}
