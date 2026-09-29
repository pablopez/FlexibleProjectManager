package com.flexibleprojectmanager.platform.settings.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flexibleprojectmanager.platform.settings.application.UserPreferencesUseCase.UpdatePreferencesCommand;
import com.flexibleprojectmanager.platform.settings.domain.Language;
import com.flexibleprojectmanager.platform.settings.domain.Theme;
import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

@Service
public class UserPreferencesService implements UserPreferencesUseCase {
    private final UserPreferencesRepository repository;
    private final Clock clock;

    public UserPreferencesService(UserPreferencesRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferences get(CurrentActor actor) {
        return repository.findByUserId(actor.userId()).orElseGet(() -> UserPreferences.defaults(actor.userId()));
    }

    @Override
    @Transactional
    public UserPreferences update(CurrentActor actor, UpdatePreferencesCommand command) {
        if (command.isEmpty()) throw new IllegalArgumentException("At least one preference must be supplied.");
        UserPreferences current = repository.findByUserId(actor.userId())
                .orElseGet(() -> UserPreferences.defaults(actor.userId()));
        Language language = command.languageSupplied()
                ? Language.fromApiValue(requireValue(command.language(), "language")) : current.language();
        Theme theme = command.themeSupplied()
                ? Theme.fromApiValue(requireValue(command.theme(), "theme")) : current.theme();
        return repository.save(new UserPreferences(actor.userId(), language, theme, Instant.now(clock)));
    }

    private static String requireValue(String value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " must not be null.");
        return value;
    }
}
