CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL CHECK (language IN ('EN', 'ES')),
    theme VARCHAR(6) NOT NULL CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM')),
    updated_at TIMESTAMPTZ NOT NULL
);
