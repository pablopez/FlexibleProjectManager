package com.flexibleprojectmanager.platform.shared.application.security;

import java.util.List;
import java.util.UUID;

public record CurrentActor(UUID userId, UUID organizationId, UUID memberId, List<String> permissions) {
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public void requirePermission(String permission) {
        if (!hasPermission(permission)) throw new AuthorizationException();
    }
}
