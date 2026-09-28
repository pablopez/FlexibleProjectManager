package com.flexibleprojectmanager.platform.users.application;

import java.util.List;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface ListRolesUseCase {
    List<RoleView> list(CurrentActor actor);
}
