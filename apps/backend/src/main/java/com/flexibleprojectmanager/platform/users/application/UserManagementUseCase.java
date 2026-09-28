package com.flexibleprojectmanager.platform.users.application;

import java.util.List;
import java.util.UUID;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.users.domain.User;

public interface UserManagementUseCase {
    PageResult list(CurrentActor actor, int page, int size, User.Status status);
    UserView get(CurrentActor actor, UUID userId);
    UserView update(CurrentActor actor, UUID userId, UpdateUserCommand command);
    UserView replaceRoles(CurrentActor actor, UUID userId, List<String> roles);

    record UpdateUserCommand(String displayName, boolean displayNameSupplied,
                             User.Status status, boolean statusSupplied) {}
    record PageResult(List<UserView> items, int page, int size, long total, int totalPages) {}
}
