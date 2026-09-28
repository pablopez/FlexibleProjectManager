package com.flexibleprojectmanager.platform.users.application;

import java.util.List;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

public interface CreateUserUseCase {
    UserView create(CurrentActor actor, CreateUserCommand command);

    record CreateUserCommand(String email, String displayName, String password, List<String> roles) {}
}
