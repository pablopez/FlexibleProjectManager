CREATE TABLE audit_events (
    id UUID PRIMARY KEY NOT NULL,
    organization_id UUID NOT NULL,
    actor_user_id UUID,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id UUID,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_org_created ON audit_events (organization_id, created_at, id);
CREATE INDEX idx_audit_org_action ON audit_events (organization_id, action, created_at);
CREATE INDEX idx_audit_org_resource ON audit_events (organization_id, resource_type, resource_id, created_at);
CREATE INDEX idx_audit_org_actor ON audit_events (organization_id, actor_user_id, created_at);
