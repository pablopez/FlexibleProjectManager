CREATE TABLE user_preferences (
    user_id TEXT PRIMARY KEY NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language TEXT NOT NULL CHECK (language IN ('EN', 'ES')),
    theme TEXT NOT NULL CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM')),
    updated_at TEXT NOT NULL
);
