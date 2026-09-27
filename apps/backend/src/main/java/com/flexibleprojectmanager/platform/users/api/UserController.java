package com.flexibleprojectmanager.platform.users.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import com.flexibleprojectmanager.platform.users.application.UserManagementRepository.RoleData;
import com.flexibleprojectmanager.platform.users.application.UserManagementRepository.UserData;
import com.flexibleprojectmanager.platform.users.application.UserManagementService;
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
    private final UserManagementService service;
    public UserController(UserManagementService service) { this.service = service; }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('users:read')")
    public UserListResponse list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String status) {
        var result = service.list(Math.max(0, page), Math.min(Math.max(1, size), 100), status);
        int actualSize = Math.min(Math.max(1, size), 100);
        return new UserListResponse(result.items().stream().map(this::user).toList(), Math.max(0, page), actualSize, result.total(), (int) Math.ceil((double) result.total() / actualSize));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('users:create')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return user(service.create(request.email(), request.displayName(), request.password(), request.roles()));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('users:read')")
    public UserResponse get(@PathVariable UUID userId) { return user(service.get(userId)); }

    @PatchMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('users:update')")
    public UserResponse update(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) { return user(service.update(userId, request.displayName(), request.status())); }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('users:update')")
    public UserResponse roles(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRolesRequest request) { return user(service.replaceRoles(userId, request.roles())); }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('users:read')")
    public RoleListResponse roles() { return new RoleListResponse(service.roles().stream().map(this::role).toList()); }

    private UserResponse user(UserData value) { return new UserResponse(value.id(), value.email(), value.displayName(), value.status(), value.roles(), value.createdAt(), value.updatedAt()); }
    private RoleResponse role(RoleData value) { return new RoleResponse(value.code(), value.name(), value.description(), value.permissions()); }
}
