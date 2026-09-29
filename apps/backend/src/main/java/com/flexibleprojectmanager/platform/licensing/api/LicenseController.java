package com.flexibleprojectmanager.platform.licensing.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flexibleprojectmanager.platform.licensing.application.LicenseManagementUseCase;
import com.flexibleprojectmanager.platform.licensing.application.LicenseManagementUseCase.EntitlementsView;
import com.flexibleprojectmanager.platform.licensing.application.LicenseManagementUseCase.LicenseView;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

import static com.flexibleprojectmanager.platform.licensing.api.LicenseDtos.*;

@RestController
@RequestMapping("/api/v1/license")
public class LicenseController {
    private final LicenseManagementUseCase licenses;
    private final CurrentActorProvider actors;

    public LicenseController(LicenseManagementUseCase licenses, CurrentActorProvider actors) {
        this.licenses = licenses;
        this.actors = actors;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('license:read')")
    public LicenseResponse get() {
        return response(licenses.get(actors.currentActor()));
    }

    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('license:manage')")
    public LicenseResponse activate(@RequestBody ActivateLicenseRequest request) {
        return response(licenses.activate(actors.currentActor(), request == null ? null : request.signedLicense()));
    }

    @PostMapping("/deactivate")
    @PreAuthorize("hasAuthority('license:manage')")
    public ResponseEntity<Void> deactivate() {
        licenses.deactivate(actors.currentActor());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entitlements")
    @PreAuthorize("hasAuthority('license:read')")
    public EntitlementsResponse entitlements() {
        EntitlementsView view = licenses.entitlements(actors.currentActor());
        return new EntitlementsResponse(view.maxUsers(), view.licenseFeatures());
    }

    private LicenseResponse response(LicenseView view) {
        return new LicenseResponse(view.licenseId(), view.organizationId(), view.installationId(), view.status(),
                view.type(), view.issuedAt(), view.expiresAt(), view.licenseFeatures(),
                view.maxUsers() == null ? null : new LicenseLimits(view.maxUsers()), view.signatureAlgorithm());
    }
}
