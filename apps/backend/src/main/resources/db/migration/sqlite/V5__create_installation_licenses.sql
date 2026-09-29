CREATE TABLE installation_licenses (
    installation_id TEXT PRIMARY KEY NOT NULL REFERENCES installations(id),
    signed_license TEXT NOT NULL,
    activated_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
