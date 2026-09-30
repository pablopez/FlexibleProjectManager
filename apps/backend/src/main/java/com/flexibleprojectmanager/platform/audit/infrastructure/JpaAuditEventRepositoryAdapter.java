package com.flexibleprojectmanager.platform.audit.infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.flexibleprojectmanager.platform.audit.application.AuditEventRepository;
import com.flexibleprojectmanager.platform.audit.application.AuditRecorder;
import com.flexibleprojectmanager.platform.audit.application.AuditQuery;
import com.flexibleprojectmanager.platform.audit.application.AuditQueryRepository;
import com.flexibleprojectmanager.platform.audit.application.AuditQueryUseCase;
import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;
import com.flexibleprojectmanager.platform.users.infrastructure.SpringDataUserRepository;

@Repository
public class JpaAuditEventRepositoryAdapter implements AuditEventRepository, AuditRecorder, AuditQueryRepository {
    private static final Sort ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
    private final SpringDataAuditEventRepository events;
    private final SpringDataUserRepository users;

    public JpaAuditEventRepositoryAdapter(SpringDataAuditEventRepository events, SpringDataUserRepository users) {
        this.events = events; this.users = users;
    }

    @Override public void append(AuditEvent event) { events.save(AuditEventJpaEntity.from(event, AuditMetadataJson.write(event.metadata()))); }
    @Override public void record(AuditEvent event) { append(event); }

    @Override
    public PageResult find(UUID organizationId, AuditQuery query) {
        var page = events.search(organizationId, query.userId(), query.action(), query.resourceType(), query.resourceId(), query.from(), query.to(),
                PageRequest.of(query.page(), query.size(), ORDER));
        Map<UUID, String> names = new HashMap<>();
        var actorIds = page.getContent().stream().map(AuditEventJpaEntity::actorUserId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        users.findByIdIn(actorIds).forEach(user -> names.put(user.id(), user.displayName()));
        return new PageResult(page.getContent().stream().map(event -> new AuditQueryUseCase.Entry(event.id(),
                event.actorUserId() == null || !names.containsKey(event.actorUserId()) ? null : new AuditQueryUseCase.Actor(event.actorUserId(), names.get(event.actorUserId())),
                event.action(), event.resourceType(), event.resourceId(), AuditMetadataJson.read(event.metadata()), event.createdAt())).toList(), page.getTotalElements());
    }
}
