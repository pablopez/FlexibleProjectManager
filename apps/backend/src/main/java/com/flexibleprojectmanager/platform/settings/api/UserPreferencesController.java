package com.flexibleprojectmanager.platform.settings.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flexibleprojectmanager.platform.settings.api.UserPreferencesDtos.UpdateUserSettingsRequest;
import com.flexibleprojectmanager.platform.settings.api.UserPreferencesDtos.UserSettingsResponse;
import com.flexibleprojectmanager.platform.settings.application.UserPreferencesUseCase;
import com.flexibleprojectmanager.platform.settings.application.UserPreferencesUseCase.UpdatePreferencesCommand;
import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActorProvider;

@RestController
@RequestMapping("/api/v1/settings/user")
public class UserPreferencesController {
    private final UserPreferencesUseCase useCase;
    private final CurrentActorProvider actors;

    public UserPreferencesController(UserPreferencesUseCase useCase, CurrentActorProvider actors) {
        this.useCase = useCase;
        this.actors = actors;
    }

    @GetMapping
    public UserSettingsResponse get() { return response(useCase.get(actors.currentActor())); }

    @PatchMapping
    public UserSettingsResponse update(@RequestBody UpdateUserSettingsRequest request) {
        return response(useCase.update(actors.currentActor(), new UpdatePreferencesCommand(
                request.language(), request.languageSupplied(), request.theme(), request.themeSupplied())));
    }

    private UserSettingsResponse response(UserPreferences value) {
        return new UserSettingsResponse(value.language().apiValue(), value.theme().apiValue());
    }
}
