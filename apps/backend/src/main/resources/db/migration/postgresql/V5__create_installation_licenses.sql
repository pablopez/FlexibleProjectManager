CREATE TABLE installation_licenses (
    installation_id UUID PRIMARY KEY REFERENCES installations(id),
    signed_license TEXT NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
