package com.flexibleprojectmanager.platform.users.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.users.domain.User;

public record UserView(UUID id, String email, String displayName, User.Status status,
                       List<String> roles, Instant createdAt, Instant updatedAt) {}
