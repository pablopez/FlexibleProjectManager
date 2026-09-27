package com.flexibleprojectmanager.platform.users.application;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    private final UserManagementRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserManagementRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserManagementRepository.PageResult<UserManagementRepository.UserData> list(int page, int size, String status) {
        return repository.findUsers(page, size, status);
    }

    public UserManagementRepository.UserData get(UUID id) {
        return repository.findUser(id).orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public UserManagementRepository.UserData create(String email, String displayName, String password, List<String> roles) {
        return repository.createUser(email.trim().toLowerCase(), displayName.trim(), passwordEncoder.encode(password), roles);
    }

    @Transactional
    public UserManagementRepository.UserData update(UUID id, String displayName, String status) {
        get(id);
        return repository.updateUser(id, displayName == null ? null : displayName.trim(), status);
    }

    @Transactional
    public UserManagementRepository.UserData replaceRoles(UUID id, List<String> roles) {
        get(id);
        return repository.replaceRoles(id, roles);
    }

    public List<UserManagementRepository.RoleData> roles() {
        return repository.findRoles();
    }
}
