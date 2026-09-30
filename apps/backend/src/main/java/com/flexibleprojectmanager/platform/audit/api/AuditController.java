package com.flexibleprojectmanager.platform.audit.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.flexibleprojectmanager.platform.audit.application.AuditQuery;
import com.flexibleprojectmanager.platform.audit.application.AuditQueryUseCase;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
    private final AuditQueryUseCase useCase;
    private final CurrentActorProvider actors;
    public AuditController(AuditQueryUseCase useCase, CurrentActorProvider actors) { this.useCase = useCase; this.actors = actors; }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public AuditDtos.ListResponse list(@RequestParam(required = false) UUID userId, @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType, @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return AuditDtos.ListResponse.from(useCase.list(actors.currentActor(), new AuditQuery(userId, action, resourceType, resourceId, from, to, page, size)));
    }
}
