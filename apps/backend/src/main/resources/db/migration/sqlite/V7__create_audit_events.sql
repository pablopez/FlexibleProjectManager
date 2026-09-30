CREATE TABLE audit_events (
    id TEXT PRIMARY KEY NOT NULL,
    organization_id TEXT NOT NULL,
    actor_user_id TEXT,
    action TEXT NOT NULL CHECK (length(action) <= 64),
    resource_type TEXT NOT NULL CHECK (length(resource_type) <= 64),
    resource_id TEXT,
    metadata TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX idx_audit_org_created ON audit_events (organization_id, created_at, id);
CREATE INDEX idx_audit_org_action ON audit_events (organization_id, action, created_at);
CREATE INDEX idx_audit_org_resource ON audit_events (organization_id, resource_type, resource_id, created_at);
CREATE INDEX idx_audit_org_actor ON audit_events (organization_id, actor_user_id, created_at);
