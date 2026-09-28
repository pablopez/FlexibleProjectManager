package com.flexibleprojectmanager.platform.users.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record OrganizationMember(UUID id, UUID userId, UUID organizationId, Status status,
                                  Instant joinedAt, Set<String> roles) {
    public enum Status { ACTIVE, DISABLED }
}
