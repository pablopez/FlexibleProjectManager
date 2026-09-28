package com.flexibleprojectmanager.platform.users.application;

import java.util.List;

public record RoleView(String code, String name, String description, List<String> permissions) {}
