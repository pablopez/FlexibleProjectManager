package com.flexibleprojectmanager.platform.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.flexibleprojectmanager.platform.settings.application.UserPreferencesRepository;
import com.flexibleprojectmanager.platform.settings.application.UserPreferencesService;
import com.flexibleprojectmanager.platform.settings.application.UserPreferencesUseCase.UpdatePreferencesCommand;
import com.flexibleprojectmanager.platform.settings.domain.Language;
import com.flexibleprojectmanager.platform.settings.domain.Theme;
import com.flexibleprojectmanager.platform.settings.domain.UserPreferences;
import com.flexibleprojectmanager.platform.shared.application.security.CurrentActor;

class UserPreferencesServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentActor ACTOR = new CurrentActor(USER_ID, UUID.randomUUID(), UUID.randomUUID(), java.util.List.of());
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void missingRowReturnsEnglishLightWithoutCreatingRow() {
        FakeRepository repository = new FakeRepository();
        UserPreferences value = service(repository).get(ACTOR);
        assertEquals(Language.EN, value.language());
        assertEquals(Theme.LIGHT, value.theme());
        assertEquals(0, repository.saves);
    }

    @Test
    void partialUpdatesUseDefaultsAndPreserveExistingValues() {
        FakeRepository repository = new FakeRepository();
        UserPreferencesService service = service(repository);
        UserPreferences languageOnly = service.update(ACTOR, new UpdatePreferencesCommand("es", true, null, false));
        assertEquals(Language.ES, languageOnly.language());
        assertEquals(Theme.LIGHT, languageOnly.theme());
        UserPreferences themeOnly = service.update(ACTOR, new UpdatePreferencesCommand(null, false, "dark", true));
        assertEquals(Language.ES, themeOnly.language());
        assertEquals(Theme.DARK, themeOnly.theme());
        UserPreferences both = service.update(ACTOR, new UpdatePreferencesCommand("en", true, "system", true));
        assertEquals(Language.EN, both.language());
        assertEquals(Theme.SYSTEM, both.theme());
        assertEquals(NOW, both.updatedAt());
    }

    @Test
    void invalidOrEmptyUpdatesAreRejected() {
        UserPreferencesService service = service(new FakeRepository());
        assertThrows(IllegalArgumentException.class, () -> service.update(ACTOR, new UpdatePreferencesCommand(null, false, null, false)));
        assertThrows(IllegalArgumentException.class, () -> service.update(ACTOR, new UpdatePreferencesCommand(null, true, null, false)));
        assertThrows(IllegalArgumentException.class, () -> service.update(ACTOR, new UpdatePreferencesCommand("fr", true, null, false)));
        assertThrows(IllegalArgumentException.class, () -> service.update(ACTOR, new UpdatePreferencesCommand(null, false, "blue", true)));
    }

    @Test
    void persistenceIsScopedToActorUserId() {
        FakeRepository repository = new FakeRepository();
        UserPreferencesService service = service(repository);
        service.update(ACTOR, new UpdatePreferencesCommand("es", true, null, false));
        assertEquals(USER_ID, repository.saved.userId());
    }

    @Test
    void usersCannotReadOrOverwriteEachOthersPreferences() {
        UUID otherUserId = UUID.randomUUID();
        CurrentActor otherActor = new CurrentActor(otherUserId, UUID.randomUUID(), UUID.randomUUID(), java.util.List.of());
        FakeRepository repository = new FakeRepository();
        UserPreferencesService service = service(repository);

        service.update(ACTOR, new UpdatePreferencesCommand("es", true, "dark", true));
        assertEquals(Language.EN, service.get(otherActor).language());
        assertEquals(Theme.LIGHT, service.get(otherActor).theme());

        service.update(otherActor, new UpdatePreferencesCommand("en", true, "system", true));
        assertEquals(Language.ES, service.get(ACTOR).language());
        assertEquals(Theme.DARK, service.get(ACTOR).theme());
        assertEquals(Theme.SYSTEM, service.get(otherActor).theme());
    }

    private static UserPreferencesService service(FakeRepository repository) {
        return new UserPreferencesService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class FakeRepository implements UserPreferencesRepository {
        private UserPreferences saved;
        private int saves;
        private final Map<UUID, UserPreferences> values = new HashMap<>();
        @Override public Optional<UserPreferences> findByUserId(UUID userId) {
            return Optional.ofNullable(values.get(userId));
        }
        @Override public UserPreferences save(UserPreferences preferences) { saved = preferences; values.put(preferences.userId(), preferences); saves++; return preferences; }
    }
}
