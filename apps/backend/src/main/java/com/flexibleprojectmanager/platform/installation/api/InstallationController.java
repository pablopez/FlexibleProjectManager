package com.flexibleprojectmanager.platform.installation.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flexibleprojectmanager.platform.installation.application.InstallationManagementUseCase;
import com.flexibleprojectmanager.platform.installation.application.InstallationManagementUseCase.InstallationView;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;
import com.flexibleprojectmanager.platform.installation.api.InstallationDtos.InstallationResponse;
import com.flexibleprojectmanager.platform.installation.api.InstallationDtos.UpdateInstallationRequest;

@RestController @RequestMapping("/api/v1/installation")
public class InstallationController {
    private final InstallationManagementUseCase useCase; private final CurrentActorProvider actors;
    public InstallationController(InstallationManagementUseCase useCase, CurrentActorProvider actors) { this.useCase=useCase; this.actors=actors; }
    @GetMapping @PreAuthorize("hasAuthority('organization:read')") public InstallationResponse get() { return response(useCase.get(actors.currentActor())); }
    @PatchMapping @PreAuthorize("hasAuthority('organization:update')") public InstallationResponse update(@RequestBody UpdateInstallationRequest request) {
        return response(useCase.update(actors.currentActor(), new InstallationManagementUseCase.UpdateInstallationCommand(request.name(), request.nameSupplied())));
    }
    private InstallationResponse response(InstallationView value) { return new InstallationResponse(value.id(), value.organizationId(), value.name(), value.platform(), value.applicationVersion(), value.status(), value.createdAt(), value.lastSeenAt()); }
}
