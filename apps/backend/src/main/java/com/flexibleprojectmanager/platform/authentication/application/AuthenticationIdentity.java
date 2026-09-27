package com.flexibleprojectmanager.platform.authentication.application;

import java.util.List;
import java.util.UUID;

public record AuthenticationIdentity(
        UUID userId,
        String email,
        String passwordHash,
        String displayName,
        String userStatus,
        UUID organizationId,
        String organizationName,
        String organizationStatus,
        UUID memberId,
        String memberStatus,
        List<String> roles,
        List<String> permissions) {

    public boolean eligible() {
        return "ACTIVE".equals(userStatus)
                && "ACTIVE".equals(memberStatus)
                && "ACTIVE".equals(organizationStatus);
    }
}
