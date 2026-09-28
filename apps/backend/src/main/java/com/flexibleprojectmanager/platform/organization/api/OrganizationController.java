package com.flexibleprojectmanager.platform.organization.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flexibleprojectmanager.platform.organization.application.OrganizationManagementUseCase;
import com.flexibleprojectmanager.platform.organization.application.OrganizationManagementUseCase.OrganizationView;
import com.flexibleprojectmanager.platform.organization.api.OrganizationDtos.OrganizationResponse;
import com.flexibleprojectmanager.platform.organization.api.OrganizationDtos.UpdateOrganizationRequest;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {
    private final OrganizationManagementUseCase useCase;
    private final CurrentActorProvider actors;
    public OrganizationController(OrganizationManagementUseCase useCase, CurrentActorProvider actors) { this.useCase = useCase; this.actors = actors; }
    @GetMapping @PreAuthorize("hasAuthority('organization:read')")
    public OrganizationResponse get() { return response(useCase.get(actors.currentActor())); }
    @PatchMapping @PreAuthorize("hasAuthority('organization:update')")
    public OrganizationResponse update(@RequestBody UpdateOrganizationRequest request) {
        return response(useCase.update(actors.currentActor(), new OrganizationManagementUseCase.UpdateOrganizationCommand(
                request.name(), request.nameSupplied(), request.slug(), request.slugSupplied())));
    }
    private OrganizationResponse response(OrganizationView value) { return new OrganizationResponse(value.id(), value.name(), value.slug(), value.status(), value.createdAt(), value.updatedAt()); }
}
