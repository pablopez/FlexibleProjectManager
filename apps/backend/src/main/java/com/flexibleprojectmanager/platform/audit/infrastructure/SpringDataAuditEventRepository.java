package com.flexibleprojectmanager.platform.audit.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAuditEventRepository extends JpaRepository<AuditEventJpaEntity, UUID> {
    @Query("select e from AuditEventJpaEntity e where e.organizationId = :organizationId "
        + "and (:userId is null or e.actorUserId = :userId) and (:action is null or e.action = :action) "
        + "and (:resourceType is null or e.resourceType = :resourceType) and (:resourceId is null or e.resourceId = :resourceId) "
        + "and (:fromTime is null or e.createdAt >= :fromTime) and (:toTime is null or e.createdAt <= :toTime)")
    Page<AuditEventJpaEntity> search(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId,
        @Param("action") String action, @Param("resourceType") String resourceType, @Param("resourceId") UUID resourceId,
        @Param("fromTime") Instant from, @Param("toTime") Instant to, Pageable pageable);
}
