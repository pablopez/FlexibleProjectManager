package com.flexibleprojectmanager.platform.licensing.infrastructure;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.flexibleprojectmanager.platform.licensing.application.LicenseNotActiveException;
import com.flexibleprojectmanager.platform.licensing.application.LicenseStatusProvider;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

@Component
public class LicenseEnforcementInterceptor implements HandlerInterceptor {
    private static final Set<String> PUBLIC_OR_RECOVERY = Set.of(
            "GET /api/v1/system/health",
            "GET /api/v1/setup/status",
            "POST /api/v1/setup/initialize",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/logout",
            "GET /api/v1/auth/me",
            "GET /api/v1/installation",
            "GET /api/v1/license",
            "POST /api/v1/license/activate",
            "POST /api/v1/license/deactivate",
            "GET /api/v1/license/entitlements");

    private final CurrentActorProvider actors;
    private final LicenseStatusProvider statuses;

    public LicenseEnforcementInterceptor(CurrentActorProvider actors, LicenseStatusProvider statuses) {
        this.actors = actors;
        this.statuses = statuses;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String route = request.getMethod().toUpperCase() + " " + request.getRequestURI();
        if (PUBLIC_OR_RECOVERY.contains(route)) return true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return true;
        if (statuses.evaluate(actors.currentActor().organizationId()).status()
                != com.flexibleprojectmanager.platform.licensing.domain.EffectiveLicenseStatus.ACTIVE) {
            throw new LicenseNotActiveException();
        }
        return true;
    }
}
