package com.flexibleprojectmanager.platform.users.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;
import com.flexibleprojectmanager.platform.users.application.CreateUserUseCase;
import com.flexibleprojectmanager.platform.users.application.ListRolesUseCase;
import com.flexibleprojectmanager.platform.users.application.RoleView;
import com.flexibleprojectmanager.platform.users.application.UserManagementUseCase;
import com.flexibleprojectmanager.platform.users.application.UserView;
import com.flexibleprojectmanager.platform.users.domain.User;
import com.flexibleprojectmanager.platform.users.api.UserDtos.CreateUserRequest;
import com.flexibleprojectmanager.platform.users.api.UserDtos.RoleListResponse;
import com.flexibleprojectmanager.platform.users.api.UserDtos.RoleResponse;
import com.flexibleprojectmanager.platform.users.api.UserDtos.UpdateUserRequest;
import com.flexibleprojectmanager.platform.users.api.UserDtos.UpdateUserRolesRequest;
import com.flexibleprojectmanager.platform.users.api.UserDtos.UserListResponse;
import com.flexibleprojectmanager.platform.users.api.UserDtos.UserResponse;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final CreateUserUseCase createUsers;
    private final UserManagementUseCase users;
    private final ListRolesUseCase roles;
    private final CurrentActorProvider actorProvider;

    public UserController(CreateUserUseCase createUsers, UserManagementUseCase users,
                          ListRolesUseCase roles, CurrentActorProvider actorProvider) {
        this.createUsers = createUsers; this.users = users; this.roles = roles; this.actorProvider = actorProvider;
    }

    @GetMapping("/users")
    public UserListResponse list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "25") int size,
                                 @RequestParam(required = false) User.Status status) {
        CurrentActor actor = actorProvider.currentActor();
        var result = users.list(actor, page, size, status);
        return new UserListResponse(result.items().stream().map(this::user).toList(), result.page(), result.size(), result.total(), result.totalPages());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return user(createUsers.create(actorProvider.currentActor(),
                new CreateUserUseCase.CreateUserCommand(request.email(), request.displayName(), request.password(), request.roles())));
    }

    @GetMapping("/users/{userId}")
    public UserResponse get(@PathVariable UUID userId) { return user(users.get(actorProvider.currentActor(), userId)); }

    @PatchMapping("/users/{userId}")
    public UserResponse update(@PathVariable UUID userId, @RequestBody UpdateUserRequest request) {
        User.Status status = request.statusSupplied() && request.status() != null ? parseStatus(request.status()) : null;
        return user(users.update(actorProvider.currentActor(), userId,
                new UserManagementUseCase.UpdateUserCommand(request.displayName(), request.displayNameSupplied(), status, request.statusSupplied())));
    }

    @PutMapping("/users/{userId}/roles")
    public UserResponse replaceRoles(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRolesRequest request) {
        return user(users.replaceRoles(actorProvider.currentActor(), userId, request.roles()));
    }

    @GetMapping("/roles")
    public RoleListResponse roles() { return new RoleListResponse(roles.list(actorProvider.currentActor()).stream().map(this::role).toList()); }

    private User.Status parseStatus(String value) {
        try { return User.Status.valueOf(value); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid user status."); }
    }
    private UserResponse user(UserView value) { return new UserResponse(value.id(), value.email(), value.displayName(), value.status().name(), value.roles(), value.createdAt(), value.updatedAt()); }
    private RoleResponse role(RoleView value) { return new RoleResponse(value.code(), value.name(), value.description(), value.permissions()); }
}
